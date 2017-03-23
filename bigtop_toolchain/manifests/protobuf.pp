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

class bigtop_toolchain::protobuf {

  include bigtop_toolchain::packages

  $url = "https://github.com/google/protobuf/releases/download/v2.5.0/"

  $protobuf8 = "protobuf-2.5.0.tar.gz"
  $protobuf8dir = "protobuf-2.5.0"

  exec { "download protobuf":
     cwd  => "/usr/src",
     command => "/usr/bin/wget $url/$protobuf8 && mkdir -p $protobuf8dir && /bin/tar -xvzf $protobuf8 -C $protobuf8dir --strip-components=1",
     creates => "/usr/src/$protobuf8dir"
  }

  exec { "install protobuf":
     cwd => "/usr/src/$protobuf8dir",
     command => "/usr/src/$protobuf8dir/configure --prefix=/usr && /usr/bin/make && /usr/bin/make install",
     require => EXEC["download protobuf"]
  }

}
