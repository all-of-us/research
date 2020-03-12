require 'open3'
require 'yaml'

class DeveloperEnvironment
  def initialize(options)
    @logger = options[:'logger']
    @input_file = options[:'input-file'] || './tasks/input/aou-workbench-dev-tools.yaml'
    @output_file = options[:'output-file'] || 'dev-tools-list.yaml'
    @versions = []
  end

  def list
    input_yaml = YAML.load(IO.read(@input_file))
    @logger.info(input_yaml)
    input_yaml['tools'].each do |tool|
      get_version_info(
          tool['tool'],
          Regexp.new(tool['number_regex']),
          tool['flag'],
          tool['use_stderr'])
    end

    yaml = YAML.dump(@versions)
    @logger.info(yaml)

    @logger.info("writing to output file: #{@output_file}")
    IO.write(@output_file, yaml)

    @logger.info(YAML.dump(@input))
    @versions
  end

  private

  VERSION_NOT_RECOGNIZED = '<version number not recognized>'

  # Do the work for each tool. Runs that tool's version mode and captures the output, then applies the
  # given number_regex to pull the version number itself out. Also grabs installation path via `which`.
  # There's no attempt here to encourage or discourage particular installation directories for various tools,
  # but the information is handy when debugging system inconsistencies (such as when you have more pythons than
  # you realized)
  # versions -  result hash object into which this tool is planted with its key as  its cmd invocation name (e.g. use http instead of httpie)
  # tool_cmd - tool to get the version info from
  # flag - flag to pass  to the tool to get the version info
  # use_stderr - if true, capture stderr instead of stdout for this tool
  # number_regex - optional. Regex that captures a group named 'version' when matched against a tool's long-form output
  def get_version_info(tool_cmd, number_regex = nil, flag = '-v', use_stderr = false) # number_extractor = ->(x) { x.itself })
    # @logger.info("#{tool_cmd} #{flag} #{use_stderr} #{number_regex}")
    result = { 'tool' => tool_cmd }
    full_cmd  = "#{tool_cmd} #{flag}"
    if use_stderr
      _stdout, output, _status = Open3.capture3(full_cmd)
    else
      output, _status = Open3.capture2(full_cmd)
    end
    result['version_string'] = output.strip
    result['version_number'] = extract_version_number(result['version_string'], tool_cmd, number_regex)

    installation_dir  = `which #{tool_cmd}`.strip
    result['installed_at'] = installation_dir.empty? ? INSTALLATION_NOT_FOUND : installation_dir
    @versions << result
  end

  def extract_version_number(version_string, tool_cmd, number_regex)
    if number_regex.nil?
      version_string
    else
      match_data = version_string.match(number_regex)
      version_number = ''
      if match_data && match_data.captures.size > 0
        @logger.info("Tool: #{tool_cmd} MatchData: #{match_data}")
        version_number = match_data[:version]
      end
      if version_number.nil? || version_number.empty?
        return VERSION_NOT_RECOGNIZED
      else
        return version_number
      end
    end
  end
end
