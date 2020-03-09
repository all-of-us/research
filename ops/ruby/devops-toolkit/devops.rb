#!/usr/bin/env ruby

require 'logger'
require 'optparse'
require 'ostruct'

require_relative 'tasks/monitoring_assets'
require_relative 'tasks/dashboards'
require_relative 'tasks/logs_based_metrics'
require_relative 'tasks/service_accounts'

# Single entry point for the devops framework. This is the only true Ruby Script file. The
# rest are classes.
#
# Based on input commands, this script delegates the work to  a task class, which should not need to
# know anything about environment variables such as ARGV, the run directory,  etc. Tasks should also avoid
# global variables like `logger`, so that we can control logging preferences from the top level.

# TODO(jaycarlton): work out a scheme for task-specific options (i.e. subcommands)
def parse_options
  # options = OpenStruct.new
  # options.dry_run = true
  # options.envs_file = ENV['DEVOPS_TOOLKIT_ENVIRONMENTS_FILE']
  options = {}
  OptionParser.new do |parser|
    parser.on('-t', '--task [TASK]', String, 'Task to be in in each environment')
    parser.on('-x', '--require XYLOPHONE', 'xylophone')
    parser.on('-e', '--envs-file [ENVS]', String, 'Path to environments JSON file.')
    parser.on('-s', '--source-uri [SOURCE-URI]', String, 'URI or FQ name for source asset')
    parser.on('-u', '--source-env [SOURCE-ENV]', String, 'Short name for source Environment (lowercase)')
    parser.on('-d', '--dry-run', 'Execute a dry run of the task')
  end.parse!({into: options})

  #Now raise an exception if we have not found a required arg
  raise OptionParser::MissingArgument.new('task') if options[:task].nil?
  raise OptionParser::MissingArgument.new('envs-file') if options[:'envs-file'].nil?

  options
end

def setup_logger
  logger = Logger.new(STDOUT)
  logger.formatter = proc do |severity, datetime, _progname, msg|
    "#{datetime} #{severity}: #{msg}\n"
  end
  logger.datetime_format = '%Y-%m-%d %H:%M:%S'
  logger
end

# TODO(jaycarlton): Tasks should handle parsing their own arguments, and work
# more like sub-commands. Which means we'll need a new notion of sub-command
# followed by task, e.g. ./devops.rb dashboards list --envs-file...
def run_task(options)
  options[:logger] = setup_logger
  # New tasks must be included here.
  case options[:task]
  when 'list-dashboards'
    Dashboards.new(options).list
  when 'replicate-dashboard'
    Dashboards.new(options).replicate
  when 'inventory'
    MonitoringAssets.new(options[:'envs-file'], logger)
  when 'replicate-logs-based-metric'
    LogsBasedMetrics.new(options).replicate
  when 'list-all-service-account-keys'
    monitoring_assets.inventory
  when 'delete-all-service-account-keys'
    ServiceAccounts.new(options).delete_all_keys
  else
    logger.error("Unrecognized task #{options[:task]}")
  end
end

# Begin execution
run_task(parse_options)
