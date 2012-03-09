# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# We make the assumption hadoop's data files will be located in /data/
# Puppet needs to know where they are
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
