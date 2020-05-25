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

# Function to return an array of roles given the roles mapping, array of components and role types to lookup.
# Example map:
# $map = {
#    component1 => {
#      role_type1 => ["xxx"],
#      role_type2 => ["yyy"],
#      role_type3 => ["zzz"],
#    }
# }
# calling get_roles(["component1"], ["role_type1", "role_type2"], $map) will return ["xxx", "yyy"]

Puppet::Parser::Functions.newfunction(:get_roles, :type => :rvalue) do |arguments|
  if arguments.size != 3
    fail Puppet::ParseError, "get_roles() method: Incorrect number of arguments. arguments given #{arguments.size} for 3"
  end

  components = arguments[0]
  role_types = arguments[1]
  roles_map = arguments[2]

  unless components.is_a? Array
    fail Puppet::ParseError, "get_roles(): Requires first argument to be array"
  end

  unless role_types.is_a? Array
    fail Puppet::ParseError, "get_roles(): Requires second argument to be array"
  end

  unless roles_map.is_a? Hash
    fail Puppet::ParseError, "get_roles(): Requires third argument to be hash"
  end

  roles = Array.new
  components.each do |component|
    role_types.each do |role_type|
      if roles_map.key?(component)
        component_map = roles_map[component]
        if component_map.key?(role_type)
          temp_roles = component_map[role_type]
          roles.concat(temp_roles)
        end
      else
        fail Puppet::ParseError, "get_roles(): No such component in roles_map. #{component}"
      end
    end
  end
  roles
end