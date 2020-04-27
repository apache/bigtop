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

class bigtop_toolchain::ruby {

  require bigtop_toolchain::packages

  $url = "https://cache.ruby-lang.org/pub/ruby/2.4"

  $ruby24 = "ruby-2.4.0.tar.gz"
  $ruby24dir = "ruby-2.4.0"

  exec { "download ruby":
     cwd  => "/usr/src",
     command => "/usr/bin/wget $url/$ruby24 && mkdir -p $ruby24dir && /bin/tar -xvzf $ruby24 -C $ruby24dir --strip-components=1 && cd $ruby24dir",
     creates => "/usr/src/$ruby24dir",
  }

  exec { "install ruby":
     cwd => "/usr/src/$ruby24dir",
     command => "/usr/src/$ruby24dir/configure --prefix=/usr/local --disable-shared && /usr/bin/make install",
     creates => "/usr/local/bin/ruby",
     timeout => 1800,
     require => EXEC["download ruby"]
  }
}
