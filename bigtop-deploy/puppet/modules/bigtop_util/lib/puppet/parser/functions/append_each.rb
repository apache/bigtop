# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements. See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License. You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
     
# Append a string to every element of an array

Puppet::Parser::Functions::newfunction(:append_each, :type => :rvalue) do |args|
  suffix = (args[0].is_a? Array) ? args[0].join("") : args[0]
  inputs = (args[1].is_a? Array) ? args[1] : [ args[1] ]
  inputs.map { |item| item + suffix }
end
