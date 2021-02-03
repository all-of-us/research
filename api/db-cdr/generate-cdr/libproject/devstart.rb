require_relative "../../../../aou-utils/serviceaccounts"
require_relative "../../../../aou-utils/utils/common"
require_relative "../../../libproject/wboptionsparser"
require "json"
require "set"
require "tempfile"

ENVIRONMENTS = {
  "all-of-us-workbench-test" => {
    :publisher_account => "circle-deploy-account@all-of-us-workbench-test.iam.gserviceaccount.com",
    :accessTiers => {
      "registered" => {
        :source_cdr_project => "all-of-us-ehr-dev",
        :ingest_cdr_project => "fc-aou-vpc-ingest-test",
        :dest_cdr_project => "fc-aou-cdr-synth-test",
        :auth_domain_group_email => "GROUP_all-of-us-registered-test@dev.test.firecloud.org",
      },
    }
  },
  "all-of-us-rw-staging" => {
    :publisher_account => "circle-deploy-account@all-of-us-workbench-test.iam.gserviceaccount.com",
    :accessTiers => {
      "registered" => {
        :source_cdr_project => "all-of-us-ehr-dev",
        :ingest_cdr_project => "fc-aou-vpc-ingest-staging",
        :dest_cdr_project => "fc-aou-cdr-synth-staging",
        :auth_domain_group_email => "GROUP_all-of-us-registered-staging@firecloud.org",
      },
    }
  },
  "all-of-us-rw-stable" => {
    :publisher_account => "deploy@all-of-us-rw-stable.iam.gserviceaccount.com",
    :accessTiers => {
      "registered" => {
        :source_cdr_project => "all-of-us-ehr-dev",
        :ingest_cdr_project => "fc-aou-vpc-ingest-stable",
        :dest_cdr_project => "fc-aou-cdr-synth-stable",
        :auth_domain_group_email => "GROUP_all-of-us-registered-stable@firecloud.org",
      },
    }
  },
  "all-of-us-rw-preprod" => {
    :publisher_account => "deploy@all-of-us-rw-preprod.iam.gserviceaccount.com",
    :accessTiers => {
      "registered" => {
        :source_cdr_project => "aou-res-curation-output-prod",
        :ingest_cdr_project => "fc-aou-vpc-ingest-preprod",
        :dest_cdr_project => "fc-aou-cdr-preprod",
        :auth_domain_group_email => "all-of-us-registered-preprod@firecloud.org",
      },
    }
  },
  "all-of-us-rw-prod" => {
    :publisher_account => "deploy@all-of-us-rw-prod.iam.gserviceaccount.com",
    :accessTiers => {
      "registered" => {
        :source_cdr_project => "aou-res-curation-output-prod",
        :ingest_cdr_project => "fc-aou-vpc-ingest-prod",
        :dest_cdr_project => "fc-aou-cdr-prod",
        :auth_domain_group_email => "all-of-us-registered-prod@firecloud.org",
      },
    }
  }
}

def ensure_docker(cmd_name, args=nil)
  args = (args or [])
  unless Workbench.in_docker?
    exec(*(%W{docker-compose run --rm cdr-scripts ./generate-cdr/project.rb #{cmd_name}} + args))
  end
end

def publish_cdr(cmd_name, args)
  ensure_docker cmd_name, args

  op = WbOptionsParser.new(cmd_name, args)
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
  op.opts.tier = "registered"
  op.add_option(
     "--tier [tier]",
     ->(opts, v) { opts.tier = v},
     "The access tier associated with this CDR, " +
     "e.g. registered. Default is registered."
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
  op.add_validator ->(opts) { raise ArgumentError unless opts.bq_dataset and opts.project and opts.tier }
  op.add_validator ->(opts) { raise ArgumentError.new("unsupported project: #{opts.project}") unless ENVIRONMENTS.key? opts.project }
  op.add_validator ->(opts) { raise ArgumentError.new("unsupported tier: #{opts.tier}") unless ENVIRONMENTS[opts.project][:accessTiers].key? opts.tier }
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
  tier = env.fetch(:accessTiers)[op.opts.tier]

  # TODO(RW-3208): Investigate using a temporary / impersonated SA credential instead of a key.
  key_file = Tempfile.new(["#{account}-key", ".json"], "/tmp")
  ServiceAccountContext.new(
    op.opts.project, account, key_file.path).run do
    # TODO(RW-3768): This currently leaves the user session with an activated service
    # account user. Ideally the activation would be hermetic within the docker
    # session, or else we would revert the active account after running.
    common.run_inline %W{gcloud auth activate-service-account -q --key-file #{key_file.path}}

    source_dataset = "#{tier.fetch(:source_cdr_project)}:#{op.opts.bq_dataset}"
    ingest_dataset = "#{tier.fetch(:ingest_cdr_project)}:#{op.opts.bq_dataset}"
    dest_dataset = "#{tier.fetch(:dest_cdr_project)}:#{op.opts.bq_dataset}"
    common.status "Copying from '#{source_dataset}' -> '#{ingest_dataset}' -> '#{dest_dataset}' as #{account}"

    # If you receive an error from "bq" like "Invalid JWT Signature", you may
    # need to delete cached BigQuery creds on your local machine. Try running
    # bq init --delete_credentials as recommended in the output.
    # TODO(RW-3768): Find a better solution for Google credentials in docker.

    # Copy through an intermediate project and delete after (include TTL in case later steps fail).
    # See https://docs.google.com/document/d/1EHw5nisXspJjA9yeZput3W4-vSIcuLBU5dPizTnk1i0/edit
    common.run_inline %W{bq mk -f --default_table_expiration 7200 --dataset #{ingest_dataset}}
    common.run_inline %W{./copy-bq-dataset.sh
        #{source_dataset} #{ingest_dataset} #{tier.fetch(:source_cdr_project)}
        #{table_match_filter} #{table_skip_filter}}

    common.run_inline %W{bq mk -f --dataset #{dest_dataset}}
    common.run_inline %W{./copy-bq-dataset.sh
        #{ingest_dataset} #{dest_dataset} #{tier.fetch(:ingest_cdr_project)}
        #{table_match_filter} #{table_skip_filter}}

    # Delete the intermediate dataset.
    common.run_inline %W{bq rm -r -f --dataset #{ingest_dataset}}

    auth_domain_group_email = tier.fetch(:auth_domain_group_email)

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

      if existing_groups.include?(auth_domain_group_email)
        common.status "#{auth_domain_group_email} already in ACL, skipping..."
      else
        common.status "Adding #{auth_domain_group_email} as a READER..."
        new_entry = { "groupByEmail" => auth_domain_group_email, "role" => "READER"}
        json["access"].push(new_entry)
      end

      # if the app SA's in too many groups, it won't gain READER transitively.
      # add it directly, to make sure.
      # See discussion at https://pmi-engteam.slack.com/archives/CHRN2R51N/p1609869521078200?thread_ts=1609796171.063800&cid=CHRN2R51N
      if existing_users.include?(app_sa)
        common.status "#{app_sa} already in ACL, skipping..."
      else
        common.status "Adding #{app_sa} as a READER..."
        new_entry = { "userByEmail" => app_sa, "role" => "READER"}
        json["access"].push(new_entry)
      end

      File.open(config_file.path, "w") do |f|
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
