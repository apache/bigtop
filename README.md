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

------------------------------------------------------------------------------------------------------------------------------------------------------

Bigtop (http://bigtop.apache.org/)
======

...is a project for the development of packaging and tests of the Apache Hadoop (http://hadoop.apache.org/) ecosystem.

------------------------------------------------------------------------------------------------------------------------------------------------------
 
The primary goal of Bigtop is to build a community around the packaging and interoperability testing of Hadoop-related projects. This includes testing at various levels (packaging, platform, runtime, upgrade, etc...) developed by a community with a focus on the system as a whole, rather than individual projects.

Quick overview of source code for bigtop.
=========================================

* __bigtop-deploy__ : deployment scripts, puppet stuff, VM utilities for bigtop based hadoop development.
    * __bigtop-packages__ : RPM specifications for bigtop subcomponents
* __bigtop-test-framework__ : The source code for the iTest utilities (framework used by smoke tests).
* __bigtop-tests__ : 
* __test-artifacts__ :source for tests.
* __test-execution__ :maven pom drivers for running the integration tests found in test-artifacts.
* __bigtop-toolchain__ : puppet scripts for setting up a machine which can build bigtop, sets up utils like jdk/maven/protobufs/...

Also, there is a new project underway, bigtop blueprints, which aims to create templates/examples that demonstrate/compare various hadoop ecosystem components with one another.  See 

Contributing 
============

    There are lots of ways to contribute, people with different expertise can help with various subprojects:
    
* __puppet__ : Much of the bigtop deploy and pacakging tools use puppet to bootstrap and setup a cluster.
* __groovy__ : Primary language used to write the bigtop smokes and itest framework. 
* __maven__ : Used to build bigtop smokes and also to define the high level bigtop project. 
* __RPM__ : Used to package apache hadoop ecosystem related projects into linux installable packages.
* __hadoop__ : Hadoop users can also contribute by using the bigtop smokes, improving them, and evaluating their breadth.
 
    Also opening JIRA's https://issues.apache.org/jira/browse/BIGTOP and getting started by posting on the mailing list is helpful.

What to people use bigtop for? 
==============================

You can go to the bigtop website : http://bigtop.apache.org/ for notes on how to do "common" tasks like

  * Hadoop App developers: Download a bigtop built hadoop 2.0 VM from the website, so you can have a running psuedodistributed hadoop cluster to test your code on.
  * Cluster administers or deployment gurus: Run the bigtop smoke tests to ensure that your cluster is working.
  * Vendors: Build your own hadoop distribution, customized from bigtop bits.

Getting Started
===============

Below are some recipes for getting started with using bigtop.  As bigtop has different subprojects, these recipes will continue to evolve.  
For specific questions its always a good idea to ping the mailing list at dev-subscribe@bigtop.apache.org to get some immediate feedback, or open a JIRA : 

For Users: Running the tests 
----------------------------

WARNING: since testing packages requires installing them on a live system it is highly recommended to use VMs for that.  Testing Bigtop is done using iTest framework. The tests are organized in maven submodules, with one submodule per bigtop component.  The bigtop-tests/test-execution/smokes/pom.xml defines all submodules to be tested, and each submodule is in its own directory under smokes/, for example:
 
*smokes/hadoop/pom.xml*
*smokes/hive/pom.xml*
*... and so on.*
 
* Step 1: Build the smokes with snapshots.  This ensures that all transitive dependencies etc.. are in your repo
    * mvn clean install -DskipTests -DskipITs -DperformRelease -f ./bigtop-test-framework/pom.xml
    * mvn clean install -DskipTests -DskipITs -DperformRelease -f ./test-artifacts/pom.xml
* Step 2: Now, rebuild in "offline" mode.  This will make sure that your local changes to bigtop are embeded in the changes.
    * mvn clean install -DskipTests -DskipITs -DperformRelease -o -nsu -f ./bigtop-test-framework/pom.xml
    * mvn clean install -DskipTests -DskipITs -DperformRelease -o -nsu -f ./bigtop-tests/test-artifacts/pom.xml
 
* Step 3: Now, you can run the smoke tests on your cluster.
    * Example 1: Running all the smoke tests with TRACE level logging (shows std out from each mr job). 
      * mvn clean verify -Dorg.apache.bigtop.itest.log4j.level=TRACE -f ./bigtop/bigtop-tests/test-execution/smokes/pom.xml 
    * Just running hadoop examples, nothing else.
      * mvn clean verify -D'org.apache.maven-failsafe-plugin.testInclude=**/*TestHadoopExamples*' -f bigtop-tests/test-execution/smokes/package/pom.xml
 
    Note: A minor bug/issue: you need the "testInclude" regular expression above, even if you don't want to customize the tests, 
    since existing test names don't follow the maven integration test naming convention of IT*, but instead, follow the surefire (unit test) convention of Test*.

For Users: Creating Your Own Hadoop Environment 
-----------------------------------------------

    Another common use case for bigtop is creating / setting up your own hadoop distribution.  
    For details on this, check out the bigtop-deploy/README.md file, which describes how to use the puppet repos
    to create and setup your VMs.  
    There is a current effort underway to create vagrant/docker recipes as well, which will be contained in the 
    bigtop-deploy/ package.     


For Developers: Building the entire distribution from scratch
-------------------------------------------------------------
 
Packages have been built for CentOS/RHEL 5 and 6, Fedora 17, SuSE Linux Enterprise 11, OpenSUSE12.2, Ubuntu LTS Lucid and Precise, and Ubuntu Quantal. They can probably be built for other platforms as well.Some of the binary artifacts might be compatible with other closely related distributions.
 
__On all systems, Building BigTop requires the following tools__

* All systems need these tools installed to build bigtop:

  Java JDK 1.6, Apache Forrest 0.8, Apache Ant, Apache Maven, git, subversion, autoconf, automake, liblzo2-dev, libz-dev, sharutils, libfuse-dev, libssl-dev

* Additionally, some details for specific linux versions :
  * __Debian__ based distros need these packages : build-essential dh-make debhelper devscripts, reprepro
  * __openSUSE 11.4__ needs these packages : relaxngDatatypedocbook-utils docbook-simple, asciidoc, fuse-devel, docbook5, docbook5-xsl-stylesheets, libxml2-devel, xmlformat, xmlto, libxslt, libopenssl-devel

* __Building packages__ : make [component-name]-[rpm|deb]
* __Building local YUM/APT repositories__ : make [component-name]-[yum|apt]

---------------------- 

Contact us

----------------------

    You can get in touch with us on the Bigtop mailing lists (http://bigtop.apache.org/mail-lists.html).

