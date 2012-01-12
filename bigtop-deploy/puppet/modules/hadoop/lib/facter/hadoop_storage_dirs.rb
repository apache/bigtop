Facter.add("hadoop_storage_dirs") do
  setcode do
    [ Facter.value("hadoop_storage_dir_pattern"),
      "/data/[0-9]*",
      "/mnt" ].reject(&:nil?).each do |pattern|

      storage_dirs = Dir.glob(pattern) \
        .select { |path| File.directory? path } \
        .join(";")

      break storage_dirs if storage_dirs.size > 0
    end
  end
end
