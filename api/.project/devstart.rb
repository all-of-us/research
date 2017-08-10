require_relative "common/common"

def startdb()
  c = Common.new
  env = c.load_env
  cname = "#{env.namespace}-db"

  db_volume_exists = false
  unless c.capture_stdout(%W{docker volume ls --quiet --filter name=#{cname}}).chomp == cname
    db_volume_exists = true
  end

  c.run_inline %W{docker run --name #{cname}
    -d
    --network #{env.namespace}
    -e MYSQL_ROOT_PASSWORD=root -e MYSQL_USER=workbench -e MYSQL_PASSWORD=workbench!pwd
    -e MYSQL_DATABASE=workbench
    -v #{cname}:/var/lib/mysql
    library/mysql
  }
  at_exit { c.run_inline %W{docker rm -f #{cname}} }

  if db_volume_exists
    c.status "Waiting a few seconds for the database to initialize itself..."
    sleep 15 # experimentally-derived
  end

  c.status "Running database migrations..."
  c.run_inline %W{
    docker run
      --rm
      -v gradle-cache:/root/.gradle
      --network #{env.namespace}
      -w /w/db
      -v #{ENV["PWD"]}:/w:cached
      #{env.namespace}-devserver
      ../gradlew --no-daemon update
  }
end

def serve()
  c = Common.new
  env = c.load_env

  c.docker.requires_docker

  c.run %W{docker network create #{env.namespace}}

  startdb
  cname = "#{env.namespace}-devserver"

  if not c.docker.image_exists?(cname)
    c.status "Building devserver image..."
    c.run_inline %W{docker build -t #{cname} src/dev/server}
  end

  c.status "Starting devserver container..."
  c.run_inline %W{
    docker run --name #{cname}
      --rm -it
      -v gradle-cache:/root/.gradle
      --network #{env.namespace}
      -w /w
      -v #{ENV["PWD"]}:/w:cached
      -p 8081:8081
      #{cname}
      ./gradlew --no-daemon appengineRun
  }
end

Common.register_command({
  :invocation => "start-devserver",
  :description => "Starts the development server.",
  :fn => Proc.new { |*args| serve(*args) }
})
