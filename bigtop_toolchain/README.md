Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements. See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

bigtop-toolchain
===============

##BigTop Toolchain Deployment through Puppet

Puppet module for configuring a host for building BigTop. It installs:

**Apache Ant 1.9**

**OpenJDK 1.8**

**Apache Maven 3.5**

**Gradle 2.4**

**Protobuf 2.5.0**

##Usage

These can be indivdually applied using:


	node "node1.example.com" {
	  include bigtop_toolchain::jdk
	  include bigtop_toolchain::maven
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

It will create a user jenkins with the required environment variables set for
building BigTop:
```
MAVEN_HOME=/usr/local/maven
JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk.x86_64
ANT_HOME=/usr/local/ant
GRADLE_HOME=/usr/local/gradle
PATH=$MAVEN_HOME/bin:$ANT_HOME/bin:$FORREST_HOME/bin:$GRADLE_HOME/bin:$PATH
```

If you do not want to use a puppet master this module can be applied
standalone with a command such as:

	puppet apply --modulepath=<path_to_bigtop> -e "include bigtop_toolchain::installer"
	
where <path_to_bigtop> is the cloned git repo.

## Installation of Tools for Bigtop Deployment

This is a separated set of manifests that helps to setup tools for Bigtop deployment.
The usage is as below:

	puppet apply --modulepath=<path_to_bigtop> -e "include bigtop_toolchain::deployment_tools"

By applying the snippet, Vagrant will be installed(the Docker installation will be added soon).

## Optional development tools

This isn't a part of fundamental toolchain recipes as we are trying to contain the size of CI and dev-
images of docker containers.
As Groovy isn't required (yet!) for creation of a Bigtop stack, this environment is separated for now
In case you system doesn't have already installed version of Bigtop recommended Groovy environment,
you should be able to so easily by running

	puppet apply --modulepath=<path_to_bigtop> -e "include bigtop_toolchain::development_tools"

Potentially, we'll be adding more development tools in this manifest.

## Requirements

The Ant/Maven/Forrest/Gradle sources will be downloaded automatically. If you already
have them and do not want to download them again please copy the source
.tar.gz files into /usr/src.

## Support

License: Apache License, Version 2.0

