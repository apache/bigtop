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

class hadoop-flume {
  define client {
    package { "flume":
      ensure => latest,
    } 
  }

  # It really is flume node, but node is a reserved keyword for puppet
  define agent {
    package { "flume-node":
      ensure => latest,
    } 

    service { "flume-node":
      ensure => running,
      require => Package["flume-node"],
      # FIXME: this need to be fixed in upstream flume
      hasstatus => false,
      hasrestart => true,
    }
  }

  define master {
    package { "flume-master":
      ensure => latest,
    } 

    service { "flume-master":
      ensure => running,
      require => Package["flume-node"],
      # FIXME: this need to be fixed in upstream flume
      hasstatus => true,
      hasrestart => true,
    }
  }
}
