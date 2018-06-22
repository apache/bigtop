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

set -e
set -x

prefix=$1
version=$2

knox_dir="${prefix}/usr/lib/knox"
knoxshell_dir="${prefix}/usr/lib/knox-shell"
etc_dir="${prefix}/etc/knox"
etc_knoxshell_dir="${prefix}/etc/knox-shell"

install -d -m 0755 "${prefix}/usr/lib/"
install -d -m 0755 "${etc_dir}"
install -d -m 0755 "${etc_knoxshell_dir}"
install -d -m 0755 "${prefix}/var/log/knox"

tar xf "target/${version}/knox-${version}.tar.gz" -C "${prefix}/usr/lib/"
tar xf "target/${version}/knoxshell-${version}.tar.gz" -C "${prefix}/usr/lib/"

mv "${prefix}/usr/lib/knox-${version}" "${knox_dir}"
mv "${prefix}/usr/lib/knoxshell-${version}" "${knoxshell_dir}"

mv "${knox_dir}/conf" "${etc_dir}/conf"
cp -r "${etc_dir}/conf" "${etc_dir}/conf.dist"
mv "${knoxshell_dir}/conf" "${etc_knoxshell_dir}/conf"
cp -r "${etc_knoxshell_dir}/conf" "${etc_knoxshell_dir}/conf.dist"



ln -nsf "/etc/knox/conf" "${knox_dir}/conf"
ln -nsf "/etc/knox-shell/conf" "${knoxshell_dir}/conf"