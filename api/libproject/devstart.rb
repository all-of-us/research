# Calls to common.run_inline in this file may use a quoted string purposefully
# to cause system() or spawn() to run the command in a shell. Calls with arrays
# are not run in a shell, which can break usage of the CloudSQL proxy.

require_relative "../../aou-utils/utils/common"
require_relative "../../aou-utils/workbench"
require_relative "cloudsqlproxycontext"
require_relative "gcloudcontext"
require_relative "wboptionsparser"
require "fileutils"
require "io/console"
require "json"
require "optparse"
require "ostruct"
require "tempfile"

def ensure_docker(cmd_name, args)
  unless Workbench::in_docker?
    exec *(%W{docker-compose run --rm scripts ./project.rb #{cmd_name}} + args)
  end
end

class Options < OpenStruct
end

# Creates a default command-line argument parser.
# command_name: For help text.
def create_parser(command_name)
  OptionParser.new do |parser|
    parser.banner = "Usage: ./project.rb #{command_name} [options]"
    parser
  end
end

def read_db_vars_v2(gcc)
  Workbench::assert_in_docker
  vars = Common.new.capture_stdout(%W{
    gsutil cat gs://#{gcc.project}-credentials/vars.env
  })
  if vars.empty?
    Common.new.error "Failed to read gs://#{gcc.project}-credentials/vars.env"
    exit 1
  end
  Workbench::read_vars(Common.new.capture_stdout(%W{
    gsutil cat gs://#{gcc.project}-credentials/vars.env
  }))
end

def dev_up(*args)
  common = Common.new

  account = get_auth_login_account()
  if account == nil
    raise("Please run 'gcloud auth login' before starting the server.")
  end

  at_exit { common.run_inline %W{docker-compose down} }
  common.status "Starting database..."
  common.run_inline %W{docker-compose up -d db}
  common.status "Running database migrations..."
  common.run_inline %W{docker-compose run db-migration}
  common.run_inline %W{docker-compose run db-cdr-migration}
  common.run_inline %W{docker-compose run db-data-migration}

  common.status "Updating configuration..."
  common.run_inline %W{
    docker-compose run update-config
    -Pconfig_file=../config/config_local.json
  }
  run_api(account)
end

Common.register_command({
  :invocation => "dev-up",
  :description => "Brings up the development environment, including db migrations and config " \
     "update. (You can use run-api instead if database and config are up-to-date.)",
  :fn => lambda { |*args| dev_up(*args) }
})


def run_api(account)
  common = Common.new
  # TODO(dmohs): This can be simplified now that we are using a shared service account.
  do_run_with_creds("all-of-us-workbench-test", account, nil) do |creds_file|
    common.status "Starting API. This can take a while. Thoughts on reducing development cycle time"
    common.status "are here:"
    common.status "  https://github.com/all-of-us/workbench/blob/master/api/doc/2017/dev-cycle.md"
    at_exit { common.run_inline %W{docker-compose down} }
    common.run_inline_swallowing_interrupt %W{docker-compose up api}
  end
end

def run_public_api_and_db()
  common = Common.new
  common.status "Starting database..."
  common.run_inline %W{docker-compose up -d db}
  common.status "Starting public API."
  common.run_inline_swallowing_interrupt %W{docker-compose up public-api}
end

Common.register_command({
  :invocation => "run-public-api",
  :description => "Runs the public api server (assumes database is up-to-date.)",
  :fn => lambda { |*args| run_public_api_and_db() }
})


def clean()
  common = Common.new
  common.run_inline %W{docker-compose run --rm api ./gradlew clean}
end

Common.register_command({
  :invocation => "clean",
  :description => "Runs gradle clean. Occasionally necessary before generating code from Swagger.",
  :fn => lambda { |*args| clean(*args) }
})


def run_api_and_db(*args)
  common = Common.new
  account = get_auth_login_account()
  if account == nil
    raise("Please run 'gcloud auth login' before starting the server.")
  end
  common.status "Starting database..."
  common.run_inline %W{docker-compose up -d db}
  run_api(account)
end

Common.register_command({
  :invocation => "run-api",
  :description => "Runs the api server (assumes database and config are already up-to-date.)",
  :fn => lambda { |*args| run_api_and_db(*args) }
})


def validate_swagger(cmd_name, args)
  ensure_docker cmd_name, args
  Common.new.run_inline %W{gradle validateSwagger} + args
end

Common.register_command({
  :invocation => "validate-swagger",
  :description => "Validate swagger definition files",
  :fn => lambda { |*args| validate_swagger("validate-swagger", args) }
})


def run_api_tests(cmd_name, args)
  ensure_docker cmd_name, args
  Common.new.run_inline %W{gradle test} + args
end

Common.register_command({
  :invocation => "test-api",
  :description => "Runs API tests. To run a single test, add (for example) " \
      "--tests org.pmiops.workbench.interceptors.AuthInterceptorTest",
  :fn => lambda { |*args| run_api_tests("test-api", args) }
})


def run_public_api_tests(cmd_name, args)
  ensure_docker cmd_name, args
  Dir.chdir('../public-api') do
    Common.new.run_inline %W{gradle test} + args
  end
end

Common.register_command({
  :invocation => "test-public-api",
  :description => "Runs public API tests. To run a single test, add (for example) " \
      "--tests org.pmiops.workbench.cdr.dao.AchillesAnalysisDaoTest",
  :fn => lambda { |*args| run_public_api_tests("test-public-api", args) }
})


def run_all_tests(cmd_name, args)
  run_api_tests(cmd_name, args)
  run_public_api_tests(cmd_name, args)
end

Common.register_command({
  :invocation => "test",
  :description => "Runs all tests (api and public-api). To run a single test, add (for example) " \
      "--tests org.pmiops.workbench.interceptors.AuthInterceptorTest",
  :fn => lambda { |*args| run_all_tests("test", args) }
})


def run_integration_tests(*args)
  common = Common.new

  account = get_auth_login_account()
  do_run_with_creds("all-of-us-workbench-test", account, nil) do |creds_file|
    common.run_inline %W{docker-compose run --rm api ./gradlew integration} + args
  end
end

Common.register_command({
  :invocation => "integration",
  :description => "Runs integration tests.",
  :fn => lambda { |*args| run_integration_tests(*args) }
})


def run_bigquery_tests(*args)
  common = Common.new

  account = get_auth_login_account()
  do_run_with_creds("all-of-us-workbench-test", account, nil) do |creds_file|
    common.run_inline %W{docker-compose run --rm api ./gradlew bigquerytest} + args
  end
end

Common.register_command({
  :invocation => "bigquerytest",
  :description => "Runs bigquerytest tests.",
  :fn => lambda { |*args| run_bigquery_tests(*args) }
})


def run_gradle(cmd_name, args)
  ensure_docker cmd_name, args
  begin
    Common.new.run_inline %W{gradle} + args
  ensure
    if $! && $!.status != 0
      Common.new.error "Command exited with non-zero status"
      exit 1
    end
  end
end

Common.register_command({
  :invocation => "gradle",
  :description => "Runs gradle inside the API docker container with the given arguments.",
  :fn => lambda { |*args| run_gradle("gradle", args) }
})


def connect_to_db(*args)
  common = Common.new

  cmd = "MYSQL_PWD=root-notasecret mysql --database=workbench"
  common.run_inline %W{docker-compose exec db sh -c #{cmd}}
end

Common.register_command({
  :invocation => "connect-to-db",
  :description => "Connect to the running database via mysql.",
  :fn => lambda { |*args| connect_to_db(*args) }
})


def docker_clean(*args)
  common = Common.new

  docker_images = `docker ps -aq`.gsub(/\s+/, " ")
  if !docker_images.empty?
    common.run_inline("docker rm -f #{docker_images}")
  end
  common.run_inline %W{docker-compose down --volumes}
end

Common.register_command({
  :invocation => "docker-clean",
  :description => \
    "Removes docker containers and volumes, allowing the next `dev-up` to" \
    " start from scratch (e.g., the database will be re-created). Includes ALL" \
    " docker images, not just for the API.",
  :fn => lambda { |*args| docker_clean(*args) }
})

def rebuild_image(*args)
  common = Common.new

  common.run_inline %W{docker-compose build}
end

Common.register_command({
  :invocation => "rebuild-image",
  :description => "Re-builds the dev docker image (necessary when Dockerfile is updated).",
  :fn => lambda { |*args| rebuild_image(*args) }
})


def get_service_account_creds_file(project, account, creds_file)
  common = Common.new
  service_account = "#{project}@appspot.gserviceaccount.com"
  common.run_inline %W{gcloud iam service-accounts keys create #{creds_file.path}
    --iam-account=#{service_account} --project=#{project} --account=#{account}}
end

def delete_service_accounts_creds(project, account, creds_file)
  tmp_private_key = `grep private_key_id #{creds_file.path} | cut -d\\\" -f4`.strip()
  service_account ="#{project}@appspot.gserviceaccount.com"
  common = Common.new
  common.run_inline %W{gcloud iam service-accounts keys delete #{tmp_private_key} -q
     --iam-account=#{service_account} --project=#{project} --account=#{account}}
  creds_file.unlink
end

def activate_service_account(creds_file)
  common = Common.new
  common.run_inline %W{gcloud auth activate-service-account --key-file #{creds_file}}
end

def copy_file_to_gcs(source_path, bucket, filename)
  common = Common.new
  common.run_inline %W{gsutil cp #{source_path} gs://#{bucket}/#{filename}}
end

def get_file_from_gcs(bucket, filename, target_path)
  common = Common.new
  common.run_inline %W{gsutil cp gs://#{bucket}/#{filename} #{target_path}}
end

# Downloads database credentials from GCS and parses them into ENV.
def read_db_vars(creds_file, project)
  db_creds_file = Tempfile.new("#{project}-vars.env")
  begin
    activate_service_account(creds_file)
    get_file_from_gcs("#{project}-credentials", "vars.env", db_creds_file.path)
    ENV.update(Workbench::read_vars_file(db_creds_file.path))
  ensure
    db_creds_file.unlink
  end
  ENV["DB_PORT"] = "3307"
end

def run_cloud_sql_proxy(project, creds_file)
  common = Common.new
  if !File.file?("cloud_sql_proxy")
    op_sys = "linux"
    if RUBY_PLATFORM.downcase.include? "darwin"
      op_sys = "darwin"
    end
    common.run_inline %W{wget https://dl.google.com/cloudsql/cloud_sql_proxy.#{op_sys}.amd64 -O
      cloud_sql_proxy}
    common.run_inline %W{chmod +x cloud_sql_proxy}
  end
  cloud_sql_proxy_cmd = %W{./cloud_sql_proxy
      -instances #{project}:us-central1:workbenchmaindb=tcp:3307
      -credential_file=#{creds_file} &}
  puts "Running Cloud SQL Proxy with #{cloud_sql_proxy_cmd}. Note stdout/err not shown."
  pid = spawn(*cloud_sql_proxy_cmd)
  sleep 3.0  # Wait for the proxy to become active.
  puts "Cloud SQL Proxy running (PID #{pid})."
  return pid
end

# Common.run_inline uses spawn() which doesn't handle pipes/redirects.
def run_with_redirects(command_string, to_redact = "")
  common = Common.new
  command_to_echo = command_string.clone
  if to_redact
    command_to_echo.sub! to_redact, "*" * to_redact.length
  end
  common.put_command(command_to_echo)
  unless system(command_string)
    raise("Error running: " + command_to_echo)
  end
end

def get_auth_login_account()
  return `gcloud config get-value account`.strip()
end

# Fetches a credentials file. Passes the path of the credentials to a block.
# For all-of-us-workbench-test only, it leaves the (lazy-fetched) creds on disk;
# for any other project, it cleans them up after the block is run.
def do_run_with_creds(project, account, creds_file)
  if creds_file == nil
    service_account_creds_file = Tempfile.new("#{project}-creds.json")
    if project == "all-of-us-workbench-test"
      creds_filename = "src/main/webapp/WEB-INF/sa-key.json"
      # For test, use a locally stored key file copied from GCS (which we leave hanging
      # around.)
      if !File.file?(creds_filename)
        # Create a temporary creds file for accessing GCS.
        get_service_account_creds_file(project, account, service_account_creds_file)
        begin
          activate_service_account(service_account_creds_file.path)
          # Copy the stable creds file from its path in GCS to sa-key.json.
          # Important: we must leave this key file in GCS, and not delete it in Cloud Console,
          # or local development will stop working.
          get_file_from_gcs("all-of-us-workbench-test-credentials",
              "all-of-us-workbench-test-9b5c623a838e.json", creds_filename)
        ensure
          # Delete the temporary creds we created.
          delete_service_accounts_creds(project, account, service_account_creds_file)
        end
      end
      yield(creds_filename)
    else
      # Create a creds file and use it; clean up when done.
      get_service_account_creds_file(project, account, service_account_creds_file)
      begin
        yield(service_account_creds_file.path)
      ensure
        delete_service_accounts_creds(project, account, service_account_creds_file)
      end
    end
  else
    yield(creds_file)
  end
end

def register_service_account(*args)
  GcloudContext.new("register-service-account", args).run do |ctx|
    Dir.chdir("../firecloud-tools") do
      ctx.common.run_inline(
          "./run.sh register_service_account/register_service_account.py" \
          " -j #{cts.opts.creds_file} -o #{ctx.opts.account}")
    end
  end
end

Common.register_command({
  :invocation => "register-service-account",
  :description => "Registers a service account with Firecloud; do this once per account we use.",
  :fn => lambda { |*args| register_service_account(*args) }
})


def drop_cloud_db(*args)
  GcloudContext.new("drop-cloud-db", args, true).run do |ctx|
    puts "Dropping database..."
    pw = ENV["MYSQL_ROOT_PASSWORD"]
    run_with_redirects(
        "cat db/drop_db.sql | envsubst | " \
        "mysql -u \"root\" -p\"#{pw}\" --host 127.0.0.1 --port 3307",
        to_redact=pw)
  end
end

Common.register_command({
  :invocation => "drop-cloud-db",
  :description => "Drops the Cloud SQL database for the specified project",
  :fn => lambda { |*args| drop_cloud_db(*args) }
})

def drop_cloud_cdr(*args)
  GcloudContext.new("drop-cloud-cdr", args, true).run do |ctx|
    puts "Dropping cdr database..."
    pw = ENV["MYSQL_ROOT_PASSWORD"]
    run_with_redirects(
        "cat db-cdr/drop_db.sql | envsubst | " \
        "mysql -u \"root\" -p\"#{pw}\" --host 127.0.0.1 --port 3307",
        to_redact=pw)
  end
end

Common.register_command({
  :invocation => "drop-cloud-cdr",
  :description => "Drops the cdr schema of Cloud SQL database for the specified project",
  :fn => lambda { |*args| drop_cloud_cdr(*args) }
})


def run_local_all_migrations(*args)
  common = Common.new

  common.run_inline %W{docker-compose run db-migration}
  common.run_inline %W{docker-compose run db-cdr-migration}
  common.run_inline %W{docker-compose run db-cdr-data-migration}
  common.run_inline %W{docker-compose run db-data-migration}
end

Common.register_command({
  :invocation => "run-local-all-migrations",
  :description => "Runs local data/schema migrations for cdr/workbench schemas.",
  :fn => lambda { |*args| run_local_all_migrations(*args) }
})


def run_local_data_migrations(*args)
  common = Common.new

  common.run_inline %W{docker-compose run db-cdr-data-migration}
  common.run_inline %W{docker-compose run db-data-migration}
end

Common.register_command({
  :invocation => "run-local-data-migrations",
  :description => "Runs local data migrations for cdr/workbench schemas.",
  :fn => lambda { |*args| run_local_data_migrations(*args) }
})


def run_local_bigdata_migrations(*args)
  common = Common.new
  common.run_inline %W{docker-compose run db-cdr-bigdata-migration}
end

Common.register_command({
  :invocation => "run-local-bigdata-migrations",
  :description => "Runs big data migrations for cdr schemas.",
  :fn => lambda { |*args| run_local_bigdata_migrations(*args) }
})


def generate_cdr_counts(*args)
  common = Common.new
  common.run_inline %W{docker-compose run db-generate-cdr-counts} + args
end

Common.register_command({
  :invocation => "generate-cdr-counts",
  :description => "generate-cdr-counts --bq-project <PROJECT> --bq-dataset <DATASET> --workbench-project <PROJECT> \
--public-project <PROJECT> --cdr-version=<''|YYYYMMDD> --bucket <BUCKET>
Generates databases in bigquery with data from a cdr that will be imported to mysql/cloudsql to be used by workbench and databrowser.",
  :fn => lambda { |*args| generate_cdr_counts(*args) }
})


def generate_local_cdr_db(*args)
  common = Common.new
  common.run_inline %W{docker-compose run db-generate-local-cdr-db} + args
end

Common.register_command({
  :invocation => "generate-local-cdr-db",
  :description => "generate-cloudsql-cdr --cdr-version <''|YYYYMMDD> --cdr-db-prefix <cdr|public> --bucket <BUCKET>
Creates and populates local mysql database from data in bucket made by generate-cdr-counts.",
  :fn => lambda { |*args| generate_local_cdr_db(*args) }
})


def generate_local_count_dbs(*args)
  common = Common.new
  common.run_inline %W{docker-compose run db-generate-local-count-dbs} + args
end

Common.register_command({
  :invocation => "generate-local-count-dbs",
  :description => "generate-local-count-dbs.sh --cdr-version <''|YYYYMMDD> --bucket <BUCKET>
Creates and populates local mysql databases cdr<VERSION> and public<VERSION> from data in bucket made by generate-cdr-counts.",
  :fn => lambda { |*args| generate_local_count_dbs(*args) }
})


def mysqldump_db(*args)
  common = Common.new
  common.run_inline %W{docker-compose run db-mysqldump-db} + args
end


Common.register_command({
  :invocation => "mysqldump-db",
  :description => "mysqldump-db db-name <LOCALDB> --bucket <BUCKET>
Dumps the local mysql db and uploads the .sql file to bucket",
  :fn => lambda { |*args| mysqldump_db(*args) }
})

def cloudsql_import(*args)
  common = Common.new
  common.run_inline %W{docker-compose run db-cloudsql-import} + args
end
Common.register_command({
                            :invocation => "cloudsql-import",
                            :description => "cloudsql-import --account <SERVICE_ACCOUNT> --project <PROJECT> --instance <CLOUDSQL_INSTANCE> --sql-dump-file <FILE.sql> --bucket <BUCKET>
Imports .sql file to cloudsql instance",
                            :fn => lambda { |*args| cloudsql_import(*args) }
                        })

def run_drop_cdr_db(*args)
  common = Common.new
  common.run_inline %W{docker-compose run drop-cdr-db}
end

Common.register_command({
  :invocation => "run-drop-cdr-db",
  :description => "Drops the cdr schema of SQL database for the specified project.",
  :fn => lambda { |*args| run_drop_cdr_db(*args) }
})


def run_cloud_data_migrations(cmd_name, args)
  ensure_docker cmd_name, args
  with_cloud_proxy_and_db_env(cmd_name, args) do
    migrate_cdr_data
    migrate_workbench_data
  end
end

Common.register_command({
  :invocation => "run-cloud-data-migrations",
  :description => "Runs data migrations in the cdr and workbench schemas on the Cloud SQL database for the specified project.",
  :fn => lambda { |*args| run_cloud_data_migrations("run-cloud-data-migrations", args) }
})


def do_create_db_creds(project, account, creds_file)
  puts "Enter the root DB user password:"
  root_password = STDIN.noecho(&:gets)
  puts "Enter the root DB user password again:"
  root_password_2 = STDIN.noecho(&:gets)
  if root_password != root_password_2
    raise("Root password entries didn't match.")
  end
  puts "Enter the workbench DB user password:"
  workbench_password = STDIN.noecho(&:gets)
  puts "Enter the workbench DB user password again:"
  workbench_password_2 = STDIN.noecho(&:gets)
  if workbench_password != workbench_password_2
    raise("Workbench password entries didn't match.")
  end

  instance_name = "#{project}:us-central1:workbenchmaindb"
  db_creds_file = Tempfile.new("#{project}-vars.env")
  if db_creds_file
    begin
      db_creds_file.puts "DB_CONNECTION_STRING=jdbc:google:mysql://#{instance_name}/workbench?rewriteBatchedStatements=true"
      db_creds_file.puts "DB_DRIVER=com.mysql.jdbc.GoogleDriver"
      db_creds_file.puts "DB_HOST=127.0.0.1"
      db_creds_file.puts "DB_NAME=workbench"
      db_creds_file.puts "CLOUD_SQL_INSTANCE=#{instance_name}"
      db_creds_file.puts "LIQUIBASE_DB_USER=liquibase"
      db_creds_file.puts "LIQUIBASE_DB_PASSWORD=#{workbench_password}"
      db_creds_file.puts "MYSQL_ROOT_PASSWORD=#{root_password}"
      db_creds_file.puts "WORKBENCH_DB_USER=workbench"
      db_creds_file.puts "WORKBENCH_DB_PASSWORD=#{workbench_password}"
      # TODO: replace with public DB, user, password
      db_creds_file.puts "PUBLIC_DB_CONNECTION_STRING=jdbc:google:mysql://#{instance_name}/cdr?rewriteBatchedStatements=true"
      db_creds_file.puts "PUBLIC_DB_USER=workbench"
      db_creds_file.puts "PUBLIC_DB_PASSWORD=#{workbench_password}"
      db_creds_file.close

      activate_service_account(creds_file)
      copy_file_to_gcs(db_creds_file.path, "#{project}-credentials", "vars.env")
    ensure
      db_creds_file.unlink
    end
  else
    raise("Error creating file.")
  end
end

def create_db_creds(*args)
  GcloudContext.new("create-db-creds", args, true).run do |ctx|
    do_create_db_creds(ctx.opts.project, ctx.opts.account, ctx.opts.creds_file)
  end
end

Common.register_command({
  :invocation => "create-db-creds",
  :description => "Creates database credentials in a file in GCS; accepts project and account args",
  :fn => lambda { |*args| create_db_creds(*args) }
})

def create_auth_domain()
  common = Common.new
  common.run_inline %W{gcloud auth login}
  token = common.capture_stdout %W{gcloud auth print-access-token}
  token = token.chomp
  header = "Authorization: Bearer #{token}"
  content_type = "Content-type: application/json"
  # TODO: make this project-specific
  common.run_inline %W{curl -X POST -H #{header} -H #{content_type} -d {}
     https://api-dot-all-of-us-workbench-test.appspot.com/v1/auth-domain/all-of-us-registered-test}
end

Common.register_command({
  :invocation => "create-auth-domain",
  :description => "Creates an authorization domain in Firecloud for registered users",
    :fn => lambda { |*args| create_auth_domain() }
})

def update_user_registered_status(cmd_name, args)
  common = Common.new
  op = WbOptionsParser.new(cmd_name, args)
  op.add_option(
    "--action [action]",
    lambda {|opts, v| opts.action = v},
    "Action to perform: add/remove."
  )
  op.add_option(
    "--account [account]",
    lambda {|opts, v| opts.account = v},
    "Account to perform update registered status as."
  )
  op.add_option(
    "--user [user]",
    lambda {|opts, v| opts.user = v},
    "User to grant or revoke registered access from."
  )
  action = op.opts.action
  account = op.opts.account
  user = op.opts.user
  op.parse.validate

  common.run_inline %W{gcloud auth login}
  token = common.capture_stdout %W{gcloud auth print-access-token}
  token = token.chomp
  common.run_inline %W{gcloud config set account #{op.opts.account}}
  header = "Authorization: Bearer #{token}"
  content_type = "Content-type: application/json"
  payload = "{\"email\": \"#{op.opts.user}\"}"
  if op.opts.action == "add"
    common.run_inline %W{curl -H #{header}
    -H #{content_type}
    -d #{payload}
    # TODO: make this project-specific
    https://api-dot-all-of-us-workbench-test.appspot.com/v1/auth-domain/all-of-us-registered-test/users}
  end

  if op.opts.action == "remove"
    common.run_inline %W{curl -X DELETE -H #{header}
    -H #{content_type}
    -d #{payload}
    https://api-dot-all-of-us-workbench-test.appspot.com/v1/auth-domain/all-of-us-registered-test/users}
  end
end

Common.register_command({
  :invocation => "update-user-registered-status",
  :description => "Adds or removes a specified user from the registered access domain.\n" \
                  "Accepts three flags: --action [add/remove], --account [admin email], and --user [target user email]",
  :fn => lambda { |*args| update_user_registered_status("update_user_registered_status", args) }
})


# Run commands with various gcloud setup/teardown: authorization and,
# optionally, a CloudSQL proxy.
class GcloudContext
  attr_reader :common, :gradlew_path, :opts

  def initialize(command_name, args, use_cloudsql_proxy = false)
    @common = Common.new
    @args = args
    @parser = create_parser(command_name)
    # Clients may access options to get default options (project etc)
    # as well as their own custom options.
    @opts = Options.new
    @use_cloudsql_proxy = use_cloudsql_proxy

    @gradlew_path = File.join(Workbench::WORKBENCH_ROOT, "api", "gradlew")
  end

  # Clients may override add_options and validate_options to add flags.
  def add_options
    @parser.on("--project [PROJECT]",
        "Project to create credentials for (e.g. all-of-us-workbench-test)") do |project|
      @opts.project = project
    end
    @parser.on("--account [ACCOUNT]",
         "Account to use when creating credentials (your.name@pmi-ops.org); "\
         "use this or --creds_file") do |account|
      @opts.account = account
    end
    @parser.on("--creds_file [CREDS_FILE]",
         "Path to a file containing credentials; use this or --account.") do |creds_file|
      @opts.creds_file = creds_file
    end
  end

  def validate_options
    if @opts.project == nil || !((@opts.account == nil) ^ (@opts.creds_file == nil))
      puts @parser.help
      exit 1
    end
  end

  # Sets up credentials (and optionally CloudSQL proxy), yields to a provided
  # block (passing the block itself / the GcloudContext), and then tears down.
  def run
    add_options
    @parser.parse @args
    validate_options
    ENV["GOOGLE_APPLICATION_CREDENTIALS"] = @opts.creds_file
    do_run_with_creds(@opts.project, @opts.account, @opts.creds_file) do |creds_file|
      @opts.creds_file = creds_file
      begin
        if @use_cloudsql_proxy
          cloudsql_proxy_pid = run_cloud_sql_proxy(@opts.project, @opts.creds_file)
          read_db_vars(@opts.creds_file, @opts.project)
        end

        yield(self)
      ensure
        if @use_cloudsql_proxy
          puts "Cleaning up CloudSQL proxy (PID #{cloudsql_proxy_pid})."
          Process.kill("HUP", cloudsql_proxy_pid)
        end
      end
    end
  end
end

# Command-line parsing and main "run" implementation for set-authority.
class SetAuthority < GcloudContext
  # Adds command-line flags specific to set-authority.
  def add_options
    super
    @parser.on(
        "--email [EMAIL,...]",
        "Comma-separated list of user accounts to change. Required."
        ) do |email|
      @opts.email = email
    end
    @parser.on(
        "--add_authority [AUTHORITY,...]",
        "Comma-separated list of user authorities to add for the users. " \
        "One of added or removed authorities is required.") do |authority|
      @opts.add_authority = authority
    end
    @parser.on(
        "--rm_authority [AUTHORITY,...]",
        "Comma-separated list of user authorities to remove from the users."
        ) do |authority|
      @opts.rm_authority = authority
    end
    @parser.on("--dry_run", "Make no changes.") do |dry_run|
      @opts.dry_run = "true"
    end
    @opts.dry_run = "false"  # default
  end

  def validate_options
    super
    if @opts.email == nil || (@opts.add_authority == nil && @opts.rm_authority == nil)
      puts @parser.help
      exit 1
    end
  end

  def run
    super do
      Dir.chdir("tools") do
        @common.run_inline %W{
            #{@gradlew_path} --info setAuthority
            -PappArgs=['#{@opts.email}','#{@opts.add_authority}','#{@opts.rm_authority}',#{@opts.dry_run}]}
      end
    end
  end
end

Common.register_command({
  :invocation => "set-authority",
  :description => "Set user authorities (permissions). See set-authority --help.",
  :fn => lambda { |*args| SetAuthority.new("set-authority", args, true).run }
})


# The test creds are always left in api/sa-key.json. This simply adds validation
# that the command is only run for the test project, and logs the path of
# the file written.
class GetTestServiceAccountCreds < GcloudContext
  def validate_options
    super
    if @opts.project != "all-of-us-workbench-test"
      raise("Only call this with all-of-us-workbench-test")
    end
  end

  def run
    super do
      puts "Creds file is now at: #{File.absolute_path(@opts.creds_file)}"
    end
  end
end

Common.register_command({
  :invocation => "get-service-creds",
  :description => "Copies sa-key.json locally (for use when running tests from an IDE, etc).",
  :fn => lambda { |*args| GetTestServiceAccountCreds.new("get-service-creds", args).run }
})

def connect_to_cloud_db(cmd_name, *args)
  ensure_docker cmd_name, args
  common = Common.new
  op = WbOptionsParser.new(cmd_name, args)
  gcc = GcloudContextV2.new(op)
  op.parse.validate
  gcc.validate
  env = read_db_vars_v2(gcc)
  CloudSqlProxyContext.new(gcc).run do
    password = env["WORKBENCH_DB_PASSWORD"]
    common.run_inline %W{
      mysql --host=127.0.0.1 --port=3307 --user=#{env["WORKBENCH_DB_USER"]}
      --database=#{env["DB_NAME"]} --password=#{password}},
      redact=password
  end
end

Common.register_command({
  :invocation => "connect-to-cloud-db",
  :description => "Connect to a Cloud SQL database via mysql.",
  :fn => lambda { |*args| connect_to_cloud_db("connect-to-cloud-db", *args) }
})


def deploy(cmd_name, args, with_cron)
  common = Common.new
  op = WbOptionsParser.new(cmd_name, args)
  op.add_option(
    "--version [version]",
    lambda {|opts, v| opts.version = v},
    "Version to deploy (e.g. your-username-test)"
  )
  op.add_option(
    "--promote",
    lambda {|opts, v| opts.promote = true},
    "Promote this deploy to make it available at the root URL"
  )
  op.add_option(
    "--no-promote",
    lambda {|opts, v| opts.promote = false},
    "Do not promote this deploy to make it available at the root URL"
  )
  op.add_option(
    "--quiet",
    lambda {|opts, v| opts.quiet = true},
    "Don't display a confirmation prompt when deploying"
  )
  gcc = GcloudContextV2.new(op)
  op.parse.validate
  gcc.validate
  env = read_db_vars_v2(gcc)
  ENV.update(env)
  common.run_inline %W{gradle :appengineStage}
  promote = op.opts.promote.nil? ? (op.opts.version ? "--no-promote" : "--promote") \
    : (op.opts.promote ? "--promote" : "--no-promote")
  quiet = op.opts.quiet ? " --quiet" : ""
  common.run_inline %W{
    gcloud app deploy
      build/staged-app/app.yaml
  } + (with_cron ? %W{build/staged-app/WEB-INF/appengine-generated/cron.yaml} : []) +
    %W{--project #{gcc.project} #{promote}} +
    (op.opts.quiet ? %W{--quiet} : []) +
    (op.opts.version ? %W{--version #{op.opts.version}} : [])
end

def deploy_api(cmd_name, args)
  ensure_docker cmd_name, args
  common = Common.new
  common.status "Deploying api..."
  deploy(cmd_name, args, with_cron=true)
end

Common.register_command({
  :invocation => "deploy-api",
  :description => "Deploys the API server to the specified cloud project.",
  :fn => lambda { |*args| deploy_api("deploy-api", args) }
})


def deploy_public_api(cmd_name, args)
  ensure_docker cmd_name, args
  common = Common.new
  common.status "Deploying public-api..."
  Dir.chdir('../public-api') do
    deploy(cmd_name, args, with_cron=false)
  end
end

Common.register_command({
  :invocation => "deploy-public-api",
  :description => "Deploys the public API server to the specified cloud project.",
  :fn => lambda { |*args| deploy_public_api("deploy-public-api", args) }
})


def migrate_database()
  common = Common.new
  common.status "Migrating main database..."
  run_with_redirects(
    "cat db/create_db.sql | envsubst | " \
    "mysql -u \"root\" -p\"#{ENV["MYSQL_ROOT_PASSWORD"]}\" --host 127.0.0.1 --port 3307",
    to_redact=ENV["MYSQL_ROOT_PASSWORD"]
  )
  Dir.chdir("db") do
    common.run_inline(%W{gradle --info update -PrunList=main})
  end
end

def migrate_workbench_data()
  common = Common.new
  common.status "Migrating workbench data..."
  run_with_redirects(
    "cat db/create_db.sql | envsubst | " \
    "mysql -u \"root\" -p\"#{ENV["MYSQL_ROOT_PASSWORD"]}\" --host 127.0.0.1 --port 3307",
    to_redact=ENV["MYSQL_ROOT_PASSWORD"]
  )
  Dir.chdir("db") do
    common.run_inline(%W{gradle --info update -PrunList=data -Pcontexts=cloud})
  end
end

def migrate_cdr_database()
  common = Common.new
  common.status "Migrating CDR database..."
  run_with_redirects(
    "cat db-cdr/create_db.sql | envsubst | " \
    "mysql -u \"root\" -p\"#{ENV["MYSQL_ROOT_PASSWORD"]}\" --host 127.0.0.1 --port 3307",
    to_redact=ENV["MYSQL_ROOT_PASSWORD"]
  )
  Dir.chdir("db-cdr") do
    common.run_inline(%W{gradle --info update -PrunList=schema})
  end
end

def migrate_cdr_data()
  common = Common.new
  common.status "Migrating CDR data..."
  run_with_redirects(
    "cat db-cdr/create_db.sql | envsubst | " \
    "mysql -u \"root\" -p\"#{ENV["MYSQL_ROOT_PASSWORD"]}\" --host 127.0.0.1 --port 3307",
    to_redact=ENV["MYSQL_ROOT_PASSWORD"]
  )
  Dir.chdir("db-cdr") do
    common.run_inline(%W{gradle --info update -PrunList=data -Pcontexts=cloud})
  end
end

def load_config(project)
  configs = {
    "all-of-us-workbench-test" => "config_test.json",
    "aou-res-workbench-stable" => "config_stable.json",
  }
  config_json = configs[project]
  unless config_json
    raise("unknown project #{project}, expected one of #{configs.keys}")
  end

  common = Common.new
  common.status "Loading #{config_json} into database..."
  Dir.chdir("tools") do
    common.run_inline %W{
      gradle --info loadConfig
      -Pconfig_file=../config/#{config_json}
    }
  end
end

def with_cloud_proxy_and_db_env(cmd_name, args)
  op = WbOptionsParser.new(cmd_name, args)
  gcc = GcloudContextV2.new(op)
  op.parse.validate
  gcc.validate
  ENV.update(read_db_vars_v2(gcc))
  ENV["DB_PORT"] = "3307" # TODO(dmohs): Use MYSQL_TCP_PORT to be consistent with mysql CLI.
  CloudSqlProxyContext.new(gcc).run do
    yield(gcc)
  end
end

def circle_deploy(cmd_name, args)
  # See https://circleci.com/docs/1.0/environment-variables/#build-details
  common = Common.new
  common.status "circle_deploy with branch='#{ENV.fetch("CIRCLE_BRANCH", "")}'" +
  " and tag='#{ENV.fetch("CIRCLE_TAG", "")}'"
  if ENV.has_key?("CIRCLE_BRANCH") and ENV.has_key?("CIRCLE_TAG")
    raise("expected exactly one of CIRCLE_BRANCH and CIRCLE_TAG env vars to be set")
  end
  is_master = ENV.fetch("CIRCLE_BRANCH", "") == "master"
  if !is_master and !ENV.has_key?("CIRCLE_TAG")
    common.status "not master or a git tag, nothing to deploy"
    return
  end

  unless Workbench::in_docker?
    exec *(%W{docker run --rm -v #{File.expand_path("..")}:/w -w /w/api
      allofustest/workbench:buildimage-0.0.9
      ./project.rb #{cmd_name}} + args)
  end

  common = Common.new
  unless File.exist? "circle-sa-key.json"
    common.error "Missing service account credentials file circle-sa-key.json."
    exit 1
  end

  if is_master
    common.status "Running database migrations..."
    with_cloud_proxy_and_db_env(cmd_name, args) do |ctx|
      migrate_database
      migrate_cdr_database
      load_config(ctx.project)
    end
  end

  promote = ""
  version = ""
  if is_master
    # Note that --promote will generally be a no-op, as we expect
    # circle-ci-test to always be serving 100% traffic. Pushing to an existing
    # live version will immediately make those changes live. In the event that
    # someone mistakenly pushes a different version manually, this --promote
    # will restore us to the expected circle-ci-test version on the next commit.
    promote = "--promote"
    version = "circle-ci-test"
  elsif ENV.has_key?("CIRCLE_TAG")
    promote = "--no-promote"
    version = ENV["CIRCLE_TAG"]
  end

  deploy_api(cmd_name, args + %W{--quiet --version #{version} #{promote}})
  deploy_public_api(cmd_name, args + %W{--quiet --version #{version} #{promote}})
end

Common.register_command({
  :invocation => "circle-deploy",
  :description => "Deploys the API server from within the Circle CI envronment.",
  :fn => lambda { |*args| circle_deploy("circle-deploy", args) }
})


def run_cloud_migrations(cmd_name, args)
  ensure_docker cmd_name, args
  with_cloud_proxy_and_db_env(cmd_name, args) { migrate_database }
end

Common.register_command({
  :invocation => "run-cloud-migrations",
  :description => "Runs database migrations on the Cloud SQL database for the specified project.",
  :fn => lambda { |*args| run_cloud_migrations("run-cloud-migrations", args) }
})


def run_cloud_cdr_migrations(cmd_name, args)
  ensure_docker cmd_name, args
  with_cloud_proxy_and_db_env(cmd_name, args) { migrate_cdr_database }
end

Common.register_command({
  :invocation => "run-cloud-cdr-migrations",
  :description => "Runs database migrations for cdr schema on the Cloud SQL database for the specified project.",
  :fn => lambda { |*args| run_cloud_cdr_migrations("run-cloud-cdr-migrations", args) }
})


def update_cloud_config(cmd_name, args)
  ensure_docker cmd_name, args
  with_cloud_proxy_and_db_env(cmd_name, args) do |ctx|
    load_config(ctx.project)
  end
end

Common.register_command({
  :invocation => "update-cloud-config",
  :description => "Updates configuration in Cloud SQL database for the specified project.",
  :fn => lambda { |*args| update_cloud_config("update-cloud-config", args) }
})

def docker_run(cmd_name, args)
  Common.new.run_inline %W{docker-compose run --rm scripts} + args
end

Common.register_command({
  :invocation => "docker-run",
  :description => "Runs the specified command in a docker container.",
  :fn => lambda { |*args| docker_run("docker-run", args) }
})
