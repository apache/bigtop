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

FROM bigtop/puppet:centos-7

# enable ssh
RUN yum -y install openssh-server openssh-clients deltarpm vim && \
    # requiretty off
    sed -i.bak 's/requiretty/!requiretty/' /etc/sudoers && \
    # setup vagrant account
    mkdir /root/.ssh && \
    chmod 0700 /root/.ssh && \
    wget http://github.com/mitchellh/vagrant/raw/master/keys/vagrant.pub --no-check-certificate -O /root/.ssh/authorized_keys && \
    chmod 0600 /root/.ssh/authorized_keys

CMD /sbin/init
