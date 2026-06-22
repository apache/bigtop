#!/bin/bash

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

init() {
    echo "`facter ipaddress` `facter fqdn`" >> /etc/hosts
    cp /etc/puppet/hieradata/site.yaml.template /etc/puppet/hieradata/site.yaml
    sed -i -e "s/head.node.fqdn/`facter fqdn`/g" /etc/puppet/hieradata/site.yaml
    puppet apply --parser future --modulepath=/bigtop-puppet/modules:/etc/puppet/modules /bigtop-puppet/manifests
}

usage() {
    echo "usage: $PROG args"
    echo "       -f, --foreground                         Running foreground."
    echo "       -b, --build                              Bootstrap the stack."
    exit 1
}

while [ $# -gt 0 ]; do
    case "$1" in
    -f|--foreground)
        init
        sleep infinity
        shift;;
    -b|--bootstrap)
        init
	shift;;
    *)
        echo "Unknown argument: '$1'" 1>&2
        usage;;
    esac
done
