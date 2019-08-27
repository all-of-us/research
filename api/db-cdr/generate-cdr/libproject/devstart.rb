require_relative "../../../../aou-utils/serviceaccounts"
require_relative "../../../../aou-utils/utils/common"
require_relative "../../../libproject/wboptionsparser"
require "json"
require "set"
require "tempfile"

ENVIRONMENTS = {
  "all-of-us-workbench-test" => {
    :publisher_account => "circle-deploy-account@all-of-us-workbench-test.iam.gserviceaccount.com",
    :source_cdr_project => "all-of-us-ehr-dev",
    :dest_cdr_project => "fc-aou-cdr-synth-test",
    :config_json => "config_test.json"
  },
  "all-of-us-rw-staging" => {
    :publisher_account => "circle-deploy-account@all-of-us-workbench-test.iam.gserviceaccount.com",
    :source_cdr_project => "all-of-us-ehr-dev",
    :dest_cdr_project => "fc-aou-cdr-synth-staging",
    :config_json => "config_staging.json"
  },
  "all-of-us-rw-stable" => {
    :publisher_account => "deploy@all-of-us-rw-stable.iam.gserviceaccount.com",
    :source_cdr_project => "all-of-us-ehr-dev",
    :dest_cdr_project => "fc-aou-cdr-synth-stable",
    :config_json => "config_stable.json"
  },
  "all-of-us-rw-prod" => {
    :publisher_account => "deploy@all-of-us-rw-prod.iam.gserviceaccount.com",
    :source_cdr_project => "aou-res-curation-output-prod",
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

def get_auth_domain_group(project)
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
  op.add_option(
    "--bq-dataset [dataset]",
    ->(opts, v) { opts.bq_dataset = v},
    "Dataset for the CDR version. Required."
  )
  op.add_option(
    "--project [project]",
    ->(opts, v) { opts.project = v},
    "The Google Cloud project associated with this environment."
  )
  op.add_validator ->(opts) { raise ArgumentError unless opts.bq_dataset and opts.project }
  op.add_validator ->(opts) { raise ArgumentError.new("unsupported project: #{opts.project}") unless ENVIRONMENTS.key? opts.project }
  op.parse.validate

  common = Common.new
  env = ENVIRONMENTS[op.opts.project]
  account = env.fetch(:publisher_account)
  key_file = Tempfile.new(["#{account}-key", ".json"], "/tmp")
  ServiceAccountContext.new(
    op.opts.project, account, key_file.path).run do
    common.run_inline %W{gcloud auth activate-service-account -q --key-file #{key_file.path}}

    source_dataset = "#{env.fetch(:source_cdr_project)}:#{op.opts.bq_dataset}"
    dest_dataset = "#{env.fetch(:dest_cdr_project)}:#{op.opts.bq_dataset}"
    common.status "Copying from '#{source_dataset}' to '#{dest_dataset}' as #{account}"

    common.run_inline %W{bq mk -f --dataset #{dest_dataset}}
    common.run_inline %W{./copy-bq-dataset.sh #{source_dataset} #{dest_dataset} #{env.fetch(:source_cdr_project)}}

    auth_domain_group = get_auth_domain_group(op.opts.project)

    config_file = Tempfile.new("#{op.opts.bq_dataset}-config.json")
    begin
      json = JSON.parse(
        common.capture_stdout %{bq show --format=prettyjson #{dest_dataset}})
      existing_groups = Set[]
      for entry in json["access"]
        if entry.key?("groupByEmail")
          existing_groups.add(entry["groupByEmail"])
        end
      end
      if existing_groups.include?(auth_domain_group)
        common.status "#{auth_domain_group} already in ACL, skipping..."
      else
        common.status "Adding #{auth_domain_group} as a READER..."
        new_entry = { "groupByEmail" => auth_domain_group, "role" => "READER"}
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
