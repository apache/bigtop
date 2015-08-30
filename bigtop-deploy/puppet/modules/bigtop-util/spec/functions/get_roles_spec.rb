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

require 'rubygems'
require 'puppetlabs_spec_helper/module_spec_helper'

describe 'get_roles' do
  let(:scope) { PuppetlabsSpec::PuppetInternals.scope }
  subject { scope.function_get_roles([components, role_types, roles_map]) }

  context 'simple test' do
    let(:components) { ["c1", "c2"] }

    let(:role_types) { ["rt1", "rt3"] }

    let(:roles_map) do
      {
        "c1" => {
          "rt1" => ["r1"],
          "rt2" => ["r2"]
        },
        "c2" => {
          "rt3" => ["r3"],
          "rt4" => ["r4"]
        },
      }
    end

    it { is_expected.to match_array(["r1", "r3"]) }
  end

  context 'missing roles_type/component test' do
      let(:components) { ["c1", "c2"] }

      let(:role_types) { ["rt1", "rt3"] }

      let(:roles_map) do
        {
          "c1" => {
            "rt1" => ["r10", "r11"],
            "rt2" => ["r2"]
          },
        }
      end

      it { is_expected.to match_array(["r10", "r11"]) }
  end
end
