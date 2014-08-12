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

bigtop-toolchain
===============

##BigTop Toolchain Deployment through Puppet

Puppet module for configuring a CentOS host for building BigTop. It installs:

**Apache Ant 1.9.4**

**Apache Forrest 0.9**

**Oracle JDK 1.6u45**

**Apache Maven 3.0.5**

**Gradle 2.0**

**Protobuf 2.5.0**

##Usage

These can be indivdually applied using:


	node "node1.example.com" {
	  include bigtop_toolchain::jdk
	  include bigtop_toolchain::maven
	  include bigtop_toolchain::forrest
	  include bigtop_toolchain::ant
	  include bigtop_toolchain::gradle
	  include bigtop_toolchain::protobuf
	  include bigtop_toolchain::packages
	  include bigtop_toolchain::env
	  include bigtop_toolchain::user
	}

Or installed as a whole with:

	node "node2.example.com" {
	  include bigtop_toolchain::installer
	}

It will create a user jenkins with the required  environment variables set for
building BigTop:

	MAVEN_HOME=/usr/local/maven
	PATH=$PATH:$MAVEN_HOME/bin
	JAVA_HOME=/usr/java/latest
	ANT_HOME=/usr/local/ant
	PATH=$PATH:$ANT_HOME/bin
	FORREST_HOME=/usr/local/apache-forrest
  GRADLE_HOME=/usr/local/gradle
  PATH=$PATH:$FORREST_HOME/bin:$GRADLE_HOME/bin

If you do not want to use a puppet master this module can be applied
standalone with a command such as:

	puppet apply --modulepath=<path_to_bigtop> -e "include bigtop_toolchain::installer"
	
where <path_to_bigtop> is the cloned git repo.

## Requirements

For RedHat/Centos, due to redistribution restrictions the Oracle JDK must be downloaded seperately. 

Download the JDK 64bit rpm.bin file, run it with the -x switch to extract the
rpm file and copy jdk-6u45-linux-amd64.rpm to files/.

Download the JDK 64 bit jdk-7u60-linux-x64.gz file into files/

The Ant/Maven/Forrest sources will be downloaded automatically. If you already
have them and do not want to download them again please copy the source
.tar.gz files into /usr/src.

## Support

License: Apache License, Version 2.0

