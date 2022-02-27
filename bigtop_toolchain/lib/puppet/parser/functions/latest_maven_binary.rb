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

module Puppet::Parser::Functions
    newfunction(:latest_maven_binary, :type => :rvalue) do |args|
        versionmask=args[0]
        # We are using main mirror here because can't be sure about Apache Server config on every mirror. It could be Nginx, btw.
        %x(curl -L --stderr /dev/null 'https://dlcdn.apache.org/maven/maven-3/?F=0&amp;V=1' | grep -o '<li>.*href="#{versionmask}/"' | tail -1 | grep -o '#{versionmask}'| tr -d '\r').chomp
    end
end
