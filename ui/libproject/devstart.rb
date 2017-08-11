require "optparse"
require_relative "utils/common"

def install_dependencies()
  common = Common.new
  common.docker.requires_docker

  parent = Dir.chdir("..") { Dir.pwd }

  common.run_inline %W{docker run --rm -w /w/ui -v #{parent}:/w:cached node npm install}
end

def swagger_regen()
  common = Common.new
  common.docker.requires_docker

  parent = Dir.chdir("..") { Dir.pwd }

  common.run_inline %W{
    docker run --rm
      -w /w -v #{parent}:/w:cached
      openjdk:jre-alpine
      java -jar tools/swagger-codegen-cli.jar generate
        --lang typescript-angular --input-spec api/src/main/resources/workbench.yaml
        --output ui/src/generated --additional-properties ngVersion=2
  }
end

class DevUpOptions
  ENV_CHOICES = %W{local test prod}
  attr_accessor :env

  def initialize
    self.env = "local"
  end

  def parse args
    parser = OptionParser.new do |parser|
      parser.banner = "Usage: ./project.rb dev-up [options]"
      parser.on(
          "--environment ENV", ENV_CHOICES, "Environment [local (default), test, prod]") do |v|
        self.env = v
      end
    end
    parser.parse args
    self
  end
end

def dev_up(*args)
  common = Common.new
  common.docker.requires_docker

  options = DevUpOptions.new.parse(args)

  unless Dir.exist?("node_modules")
    install_dependencies
  end

  swagger_regen

  ENV["ENV_FLAG"] = options.env == "local" ? "" : "--environment=#{options.env}"
  common.run_inline_swallowing_interrupt %W{docker-compose up}
  at_exit { common.run_inline %W{docker-compose down} }
end

def start_tests()
  common = Common.new
  common.docker.requires_docker

  swagger_regen

  common.status "Starting tests. Open\n"
  common.status "    http://localhost:9876/debug.html\n"
  common.status "in a browser to view/re-run."
  common.run_inline %W{
    docker run --rm -it -w /w -v #{ENV["PWD"]}:/w -p 9876:9876 node npm test
  }
end

def clean_git_hooks()
  common.run_inline %W{find ../.git/hooks -type l -delete}
end

Common.register_command({
  :invocation => "dev-up",
  :description => "Brings up the development environment.",
  :fn => Proc.new { |*args| dev_up(*args) }
})

Common.register_command({
  :invocation => "test",
  :description => "Runs the test server and opens a browser to run the tests.",
  :fn => Proc.new { |*args| start_tests(*args) }
})

Common.register_command({
  :invocation => "install-dependencies",
  :description => "Installs dependencies via npm.",
  :fn => Proc.new { |*args| install_dependencies(*args) }
})

Common.register_command({
  :invocation => "swagger-regen",
  :description => "Regenerates API client libraries from Swagger definitions.",
  :fn => Proc.new { |*args| swagger_regen(*args) }
})

Common.register_command({
  :invocation => "clean-git-hooks",
  :description => "Removes symlinks created by shared-git-hooks. Necessary before re-installing.",
  :fn => Proc.new { |*args| clean-git-hooks(*args) }
})
