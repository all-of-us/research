require_relative "../../../../aou-utils/serviceaccounts"
require_relative "../../../../aou-utils/utils/common"
require_relative "../../../libproject/wboptionsparser"
require "json"
require "set"
require "tempfile"

ENVIRONMENTS = {
  "all-of-us-workbench-test" => {
    :publisher_account => "circle-deploy-account@all-of-us-workbench-test.iam.gserviceaccount.com",
    :source_cdr_project => "all-of-us-workbench-test",
    :ingest_cdr_project => "fc-aou-vpc-ingest-test",
    :dest_cdr_project => "fc-aou-cdr-synth-test",
    :config_json => "config_test.json"
  },
  "all-of-us-rw-staging" => {
    :publisher_account => "circle-deploy-account@all-of-us-workbench-test.iam.gserviceaccount.com",
    :source_cdr_project => "all-of-us-ehr-dev",
    :ingest_cdr_project => "fc-aou-vpc-ingest-staging",
    :dest_cdr_project => "fc-aou-cdr-synth-staging",
    :config_json => "config_staging.json"
  },
  "all-of-us-rw-stable" => {
    :publisher_account => "deploy@all-of-us-rw-stable.iam.gserviceaccount.com",
    :source_cdr_project => "all-of-us-ehr-dev",
    :ingest_cdr_project => "fc-aou-vpc-ingest-stable",
    :dest_cdr_project => "fc-aou-cdr-synth-stable",
    :config_json => "config_stable.json"
  },
  "all-of-us-rw-preprod" => {
    :publisher_account => "deploy@all-of-us-rw-preprod.iam.gserviceaccount.com",
    :source_cdr_project => "aou-res-curation-output-prod",
    :ingest_cdr_project => "fc-aou-vpc-ingest-preprod",
    :dest_cdr_project => "fc-aou-cdr-preprod",
    :config_json => "config_preprod.json"
  },
  "all-of-us-rw-prod" => {
    :publisher_account => "deploy@all-of-us-rw-prod.iam.gserviceaccount.com",
    :source_cdr_project => "aou-res-curation-output-prod",
    :ingest_cdr_project => "fc-aou-vpc-ingest-prod",
    :dest_cdr_project => "fc-aou-cdr-prod",
    :config_json => "config_prod.json"
  }
}

def get_config(env)
  unless ENVIRONMENTS.fetch(env, {}).has_key?(:config_json)
    raise ArgumentError.new("env '#{env}' lacks a valid configuration")
  end
  return JSON.parse(File.read("../../config/" + ENVIRONMENTS[env][:config_json]))
end

def get_auth_domain_group_email(project)
  return get_config(project)["firecloud"]["registeredDomainGroup"]
end

def ensure_docker(cmd_name, args=nil)
  args = (args or [])
  unless Workbench.in_docker?
    exec(*(%W{docker-compose run --rm cdr-scripts ./generate-cdr/project.rb #{cmd_name}} + args))
  end
end

def publish_cdr(cmd_name, args)
  ensure_docker cmd_name, args

  op = WbOptionsParser.new(cmd_name, args)
  op.opts.exclude_sa_acl = false
  op.opts.exclude_auth_domain_acl = false

  op.add_option(
    "--bq-dataset [dataset]",
    ->(opts, v) { opts.bq_dataset = v},
    "BigQuery dataset name for the CDR version (project not included), e.g. " +
    "'2019Q4R3'. Required."
  )
  op.add_option(
    "--project [project]",
    ->(opts, v) { opts.project = v},
    "The Google Cloud project associated with this workbench environment, " +
    "e.g. all-of-us-rw-staging. Required."
  )
  op.add_option(
    "--table-prefixes [prefix1,prefix2,...]",
    ->(opts, v) { opts.table_prefixes = v},
    "Optional comma-delimited list of table prefixes to filter the publish " +
    "by, e.g. cb_,ds_. This should only be used in special situations e.g. " +
    "when the auxilliary cb_ or ds_ tables need to be updated, or if there " +
    "was an issue with the publish. In general, CDRs should be treated as " +
    "immutable after the initial publish."
  )
  op.add_typed_option(
      "--exclude-sa-acl",
      TrueClass,
      ->(opts, v) { opts.exclude_sa_acl = v},
      "When true, does not include the default service account in the ACL. Defaults to false which includes the ACL.")
  op.add_typed_option(
      "--exclude-auth-domain-acl",
      TrueClass,
      ->(opts, v) { opts.exclude_auth_domain_acl = v},
      "When true, does not include the auth domain in the ACL. Defaults to false which includes the ACL.")
  op.add_option(
      "--additional-reader-group [reader_email]",
      ->(opts, v) { opts.additional_reader_group = v},
      "Additional Google group to include in the reader ACL.")
  op.add_validator ->(opts) { raise ArgumentError unless opts.bq_dataset and opts.project }
  op.add_validator ->(opts) { raise ArgumentError.new("unsupported project: #{opts.project}") unless ENVIRONMENTS.key? opts.project }
  op.parse.validate

  # This is a grep filter. It matches all tables, by default.
  table_match_filter = ""
  if op.opts.table_prefixes
    prefixes = op.opts.table_prefixes.split(",")
    table_match_filter = "^\\(#{prefixes.join("\\|")}\\)"
  end

  # This is a grep -v filter. It skips cohort builder build-only tables, which
  # follow the convention of having the prefix prep_. See RW-4863.
  table_skip_filter = "^prep_"

  common = Common.new
  env = ENVIRONMENTS[op.opts.project]
  account = env.fetch(:publisher_account)
  app_sa = "#{op.opts.project}@appspot.gserviceaccount.com"

  # TODO(RW-3208): Investigate using a temporary / impersonated SA credential instead of a key.
  key_file = Tempfile.new(["#{account}-key", ".json"], "/tmp")
  ServiceAccountContext.new(
    op.opts.project, account, key_file.path).run do
    # TODO(RW-3768): This currently leaves the user session with an activated service
    # account user. Ideally the activation would be hermetic within the docker
    # session, or else we would revert the active account after running.
    common.run_inline %W{gcloud auth activate-service-account -q --key-file #{key_file.path}}

    source_dataset = "#{env.fetch(:source_cdr_project)}:#{op.opts.bq_dataset}"
    ingest_dataset = "#{env.fetch(:ingest_cdr_project)}:#{op.opts.bq_dataset}"
    dest_dataset = "#{env.fetch(:dest_cdr_project)}:#{op.opts.bq_dataset}"
    common.status "Copying from '#{source_dataset}' -> '#{ingest_dataset}' -> '#{dest_dataset}' as #{account}"

    # If you receive an error from "bq" like "Invalid JWT Signature", you may
    # need to delete cached BigQuery creds on your local machine. Try running
    # bq init --delete_credentials as recommended in the output.
    # TODO(RW-3768): Find a better solution for Google credentials in docker.

    # Copy through an intermediate project and delete after (include TTL in case later steps fail).
    # See https://docs.google.com/document/d/1EHw5nisXspJjA9yeZput3W4-vSIcuLBU5dPizTnk1i0/edit
    common.run_inline %W{bq mk -f --default_table_expiration 7200 --dataset #{ingest_dataset}}
    common.run_inline %W{./copy-bq-dataset.sh
        #{source_dataset} #{ingest_dataset} #{env.fetch(:source_cdr_project)}
        #{table_match_filter} #{table_skip_filter}}

    common.run_inline %W{bq mk -f --dataset #{dest_dataset}}
    common.run_inline %W{./copy-bq-dataset.sh
        #{ingest_dataset} #{dest_dataset} #{env.fetch(:ingest_cdr_project)}
        #{table_match_filter} #{table_skip_filter}}

    # Delete the intermediate dataset.
    common.run_inline %W{bq rm -r -f --dataset #{ingest_dataset}}

    auth_domain_group_email = get_auth_domain_group_email(op.opts.project)

    config_file = Tempfile.new("#{op.opts.bq_dataset}-config.json")
    begin
      json = JSON.parse(
        common.capture_stdout %{bq show --format=prettyjson #{dest_dataset}})
      existing_groups = Set[]
      existing_users = Set[]
      for entry in json["access"]
        if entry.key?("groupByEmail")
          existing_groups.add(entry["groupByEmail"])
        end
        if entry.key?("userByEmail")
          existing_users.add(entry["userByEmail"])
        end
      end

      unless op.opts.exclude_auth_domain_acl
        if existing_groups.include?(auth_domain_group_email)
          common.status "#{auth_domain_group_email} already in ACL, skipping..."
        else
          common.status "Adding #{auth_domain_group_email} as a READER..."
          new_entry = { "groupByEmail" => auth_domain_group_email, "role" => "READER"}
          json["access"].push(new_entry)
        end
      end

      # if the app SA's in too many groups, it won't gain READER transitively.
      # add it directly, to make sure.
      # See discussion at https://pmi-engteam.slack.com/archives/CHRN2R51N/p1609869521078200?thread_ts=1609796171.063800&cid=CHRN2R51N
      unless op.opts.exclude_sa_acl
        if existing_users.include?(app_sa)
          common.status "#{app_sa} already in ACL, skipping..."
        else
          common.status "Adding #{app_sa} as a READER..."
          new_entry = { "userByEmail" => app_sa, "role" => "READER"}
          json["access"].push(new_entry)
        end
      end

      if op.opts.additional_reader_group
        new_group = op.opts.additional_reader_group

        if existing_users.include?(new_group)
          common.status "#{new_group} already in ACL, skipping..."
        else
          common.status "Adding #{new_group} as a READER..."
          new_entry = { "groupByEmail" => new_group, "role" => "READER"}
          json["access"].push(new_entry)
        end
      end

      File.open(config_file.path, "w") do |f|
        f.write(JSON.pretty_generate(json))
      end

      File.open("update_acl.json", "w") do |f|
        f.write(JSON.pretty_generate(json))
      end

      common.run_inline %{bq update --source #{config_file.path} #{dest_dataset}}
    ensure
      config_file.unlink
    end
  end
end

Common.register_command({
  :invocation => "publish-cdr",
  :description => "Publishes a CDR dataset by copying it into a Firecloud CDR project and making it readable by registered users in the corresponding environment",
  :fn => ->(*args) { publish_cdr("publish-cdr", args) }
})
