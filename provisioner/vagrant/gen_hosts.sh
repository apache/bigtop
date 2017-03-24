# Licensed to Hortonworks, Inc. under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  Hortonworks, Inc. licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

echo "# Do not remove the following line, or various programs" > /etc/hosts
echo "# that require network functionality will fail." >> /etc/hosts
echo "127.0.0.1		localhost.localdomain localhost" >> /etc/hosts

function get_inet_iface(){
  route | grep default | awk '{print $8}'
}

function get_ip() {
  ip addr | grep 'inet ' | grep -E "($(get_inet_iface))" | awk '{ print $2 }' | awk -F'/' '{print $1}'
}

HOST=$(get_ip)
NUM=5
while [ -z "$HOST" ]; do
  HOST=$(get_ip)
  sleep 5
  NUM=$(($NUM-1))
  if [ $NUM -le 0 ]; then
    HOST="127.0.0.1"
    echo "Failed to update IP"
    break
  fi
done
echo "$HOST	`hostname`" >> /etc/hosts

echo 0 > /proc/sys/kernel/hung_task_timeout_secs
ethtool -K $(get_inet_iface) tso off
