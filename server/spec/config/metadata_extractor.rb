require 'exiftool_vendored'

class MetadataExtractor
  attr_reader :data

  EXIFTOOL_CMD_LINE_OPTIONS = '-s -a -u -G1'
  Exiftool.command += " #{EXIFTOOL_CMD_LINE_OPTIONS}"

  def initialize(file_path)
    @file_path = file_path
    @data = Exiftool.new(file_path)
  end
end
