require_relative "utils/common"
require "io/console"
require "optparse"
require "tempfile"

class ProjectAndAccountOptions
  attr_accessor :project
  attr_accessor :account
  attr_accessor :command
  attr_accessor :creds_file
  attr_accessor :parser

  def initialize(command)
    self.command = command
    self.parser = OptionParser.new do |parser|
      parser.banner = "Usage: ./project.rb #{self.command} [options]"
      parser.on("--project [PROJECT]",
          "Project to create credentials for (e.g. all-of-us-workbench-test)") do |project|
        self.project = project
      end
      parser.on("--account [ACCOUNT]",
           "Account to use when creating credentials (your.name@pmi-ops.org); "\
           "use this or --creds_file") do |account|
        self.account = account
      end
      parser.on("--creds_file [CREDS_FILE]",
           "Path to a file containing credentials; use this or --account.") do |creds_file|
        self.creds_file = creds_file
      end
    end
  end

  def parse args
    self.parser.parse args
    if self.project == nil || !((self.account == nil) ^ (self.creds_file == nil))
      puts self.parser.help
      raise("Invalid arguments")
    end
    self
  end
end


def dev_up(args)
  common = Common.new
  common.docker.requires_docker

  at_exit { common.run_inline %W{docker-compose down} }
  common.run_inline %W{docker-compose up -d db}
  common.run_inline %W{docker-compose run db-migration}
  common.run_inline_swallowing_interrupt %W{docker-compose up api}
end

def connect_to_db(args)
  common = Common.new
  common.docker.requires_docker

  cmd = "MYSQL_PWD=root-notasecret mysql --database=workbench"
  common.run_inline %W{docker-compose exec db sh -c #{cmd}}
end

def docker_clean(args)
  common = Common.new
  common.docker.requires_docker

  common.run_inline %W{docker-compose down --volumes}
end

def rebuild_image(args)
  common = Common.new
  common.docker.requires_docker

  common.run_inline %W{docker-compose build}
end

def get_service_account_creds_file(project, account, creds_file)
  common = Common.new
  service_account ="#{project}@appspot.gserviceaccount.com"
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

def read_db_vars(project)
  db_creds_file = Tempfile.new("#{project}-vars.env")
  begin
    get_file_from_gcs("#{project}-credentials", "vars.env", db_creds_file.path)
    db_creds_file.open
    db_creds_file.each_line do |line|
      line = line.strip()
      if !line.empty?
        parts = line.split("=")
        ENV[parts[0]] = parts[1]
      end
    end
  ensure
    db_creds_file.unlink
  end
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
  puts "Running Cloud SQL Proxy..."
  pid = spawn(*%W{./cloud_sql_proxy
      -instances #{project}:us-central1:workbenchmaindb=tcp:3307
      -credential_file=#{creds_file} &})
  common.run_inline %W{sleep 3}
  return pid
end

def do_run_migrations(project)
  read_db_vars(project)
  common = Common.new
  create_db_file = Tempfile.new("#{project}-create-db.sql")
  begin
    unless system("cat db/create_db.sql | envsubst > #{create_db_file.path}")
      raise("Error generating create_db file; exiting.")
    end
    puts "Creating database if it does not exist..."
    unless system("mysql -u \"root\" -p\"#{ENV["MYSQL_ROOT_PASSWORD"]}\" --host 127.0.0.1 "\
              "--port 3307 < #{create_db_file.path}")
      raise("Error creating database; exiting.")
    end
  ensure
    create_db_file.unlink
  end
  ENV["DB_PORT"] = "3307"
  puts "Upgrading database..."
  unless system("cd db && ../gradlew --no-daemon --info update && cd ..")
    raise("Error upgrading database. Exiting.")
  end
end

def do_drop_db(project)
  read_db_vars(project)
  common = Common.new
  drop_db_file = Tempfile.new("#{project}-drop-db.sql")
  begin
    unless system("cat db/drop_db.sql | envsubst > #{drop_db_file.path}")
      raise("Error generating drop_db file; exiting.")
    end
    puts "Dropping database..."
    unless system("mysql -u \"root\" -p\"#{ENV["MYSQL_ROOT_PASSWORD"]}\" --host 127.0.0.1 "\
              "--port 3307 < #{drop_db_file.path}")
      raise("Error dropping database; exiting.")
    end
  ensure
    drop_db_file.unlink
  end
end

def run_with_creds(args, proc)
  options = ProjectAndAccountOptions.new("drop-cloud-db").parse(args)
  project = options.project
  account = options.account
  creds_file = options.creds_file
  if creds_file == nil
    service_account_creds_file = Tempfile.new("#{project}-creds.json")
    get_service_account_creds_file(project, account, service_account_creds_file)
    begin
      proc.call(project, account, service_account_creds_file.path)
    ensure
      delete_service_accounts_creds(project, account, service_account_creds_file)
    end
  else
    proc.call(project, account, creds_file)
  end
end

def run_with_cloud_sql_proxy(args, proc)
  run_with_creds(args, Proc.new { |project, account, creds_file|
    pid = run_cloud_sql_proxy(project, creds_file)
    begin
      proc.call(project, account, creds_file)
    ensure
      Process.kill("HUP", pid)
    end
  })
end

def drop_cloud_db(args)
  run_with_cloud_sql_proxy(args, Proc.new { |project, account, creds_file|
    do_drop_db(project)
  })
end

def connect_to_cloud_db(args)
  run_with_cloud_sql_proxy(args, Proc.new { |project, account, creds_file|
    read_db_vars(project)
    system("mysql -u \"workbench\" -p\"#{ENV["WORKBENCH_DB_PASSWORD"]}\" --host 127.0.0.1 "\
           "--port 3307 --database #{ENV["DB_NAME"]}")
  })
end

def run_cloud_migrations(args)
  run_with_cloud_sql_proxy(args, Proc.new { |project, account, creds_file|
    puts "Running migrations..."
    do_run_migrations(project)
  })
end

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
      db_creds_file.puts "DB_CONNECTION_STRING=jdbc:google:mysql://#{instance_name}/workbench"
      db_creds_file.puts "DB_DRIVER=com.mysql.jdbc.GoogleDriver"
      db_creds_file.puts "DB_HOST=127.0.0.1"
      db_creds_file.puts "DB_NAME=workbench"
      db_creds_file.puts "CLOUD_SQL_INSTANCE=#{instance_name}"
      db_creds_file.puts "LIQUIBASE_DB_USER=liquibase"
      db_creds_file.puts "LIQUIBASE_DB_PASSWORD=#{workbench_password}"
      db_creds_file.puts "MYSQL_ROOT_PASSWORD=#{root_password}"
      db_creds_file.puts "WORKBENCH_DB_USER=workbench"
      db_creds_file.puts "WORKBENCH_DB_PASSWORD=#{workbench_password}"
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

def create_db_creds(args)
  run_with_creds(args, Proc.new { |project, account, creds_file|
    do_create_db_creds(project, account, creds_file)
  })
end

Common.register_command({
  :invocation => "dev-up",
  :description => "Brings up the development environment.",
  :fn => Proc.new { |args| dev_up(args) }
})

Common.register_command({
  :invocation => "connect-to-db",
  :description => "Connect to the running database via mysql.",
  :fn => Proc.new { |args| connect_to_db(args) }
})

Common.register_command({
  :invocation => "docker-clean",
  :description => \
    "Removes docker containers and volumes, allowing the next `dev-up` to\n" \
    "start from scratch (e.g., the database will be re-created).",
  :fn => Proc.new { |args| docker_clean(args) }
})

Common.register_command({
  :invocation => "rebuild-image",
  :description => "Re-builds the dev docker image (necessary when Dockerfile is updated).",
  :fn => Proc.new { |args| rebuild_image(args) }
})

Common.register_command({
  :invocation => "create-db-creds",
  :description => "Creates database credentials in a file in GCS; accepts project and account args",
  :fn => Proc.new { |args| create_db_creds(args) }
})

Common.register_command({
  :invocation => "drop-cloud-db",
  :description => "Drops the Cloud SQL database for the specified project",
  :fn => Proc.new { |args| drop_cloud_db(args) }
})

Common.register_command({
  :invocation => "run-cloud-migrations",
  :description => "Runs database migrations on the Cloud SQL database for the specified project.",
  :fn => Proc.new { |args| run_cloud_migrations(args) }
})

Common.register_command({
  :invocation => "connect-to-cloud-db",
  :description => "Connect to a Cloud SQL database via mysql.",
  :fn => Proc.new { |args| connect_to_cloud_db(args) }
})
