#!/usr/bin/env ruby

require_relative "../apidef/libproject/workbench"
require_relative "libproject/devstart.rb"

Workbench.handle_argv_or_die(__FILE__)
