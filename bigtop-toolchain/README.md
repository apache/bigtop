bigtop-toolchain
===============

##BigTop Toolchain Deployment through Puppet

Ian Mordey <ian.mordey@wandisco.com>

Puppet module for configuring a CentOS host for building BigTop. It installs:

**Apache Ant 1.9.0**

**Apache Forrest 0.9**

**Oracle JDK 1.6u43**

**Apache Maven 3.0.5**

**Protobuf 2.4.1**

##Usage

These can be indivdually applied using:


	node "node1.example.com" {
	  include bigtop-toolchain::jdk
	  include bigtop-toolchain::maven
	  include bigtop-toolchain::forrest
	  include bigtop-toolchain::ant
	  include bigtop-toolchain::protobuf
	  include bigtop-toolchain::packages
	  include bigtop-toolchain::env
	  include bigtop-toolchain::user
	}

Or installed as a whole with:

	node "node2.example.com" {
	  include bigtop-toolchain::installer
	}

It will create a user jenkins with the required  environment variables set for building BigTop:

	MAVEN_HOME=/usr/local/maven
	PATH=$PATH:$MAVEN_HOME/bin
	JAVA_HOME=/usr/java/latest
	ANT_HOME=/usr/local/ant
	PATH=$PATH:$ANT_HOME/bin
	FORREST_HOME=/usr/local/apache-forrest
	PATH=$PATH:$FORREST_HOME/bin
	
If you do not want to use a puppet master this module can be applied standalone with a command such as:

	puppet apply --modulepath=<path_to_bigtop> -e "include bigtop-toolchain::installer"
	
where <path_to_bigtop> is the cloned git repo.

## Requirements

Due to redistribution restrictions the Oracle JDK must be downloaded seperately. 

Download the JDK 64bit rpm.bin file, run it with the -x switch to extract the rpm file and copy jdk-6u43-linux-amd64.rpm to files/.

The following files must also be downloaded from their respective mirrors and copied into files/

[apache-ant-1.9.0-bin.tar.gz](http://mirrors.ibiblio.org/apache//ant/binaries/apache-ant-1.9.0-bin.tar.gz)

[apache-forrest-0.9.tar.gz](http://archive.apache.org/dist/forrest/0.9/apache-forrest-0.9.tar.gz)

[apache-maven-3.0.5-bin.tar.gz](ftp://mirror.reverse.net/pub/apache/maven/maven-3/3.0.5/binaries/apache-maven-3.0.5-bin.tar.gz)


## Support

License: Apache License, Version 2.0

