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


[Apache Bigtop](http://bigtop.apache.org/)
==========================================

...is a project for the development of packaging and tests of the [Apache Hadoop](http://hadoop.apache.org/) ecosystem.
 
The primary goal of Apache Bigtop is to build a community around the packaging and interoperability testing of Apache Hadoop-related projects. This includes testing at various levels (packaging, platform, runtime, upgrade, etc...) developed by a community with a focus on the system as a whole, rather than individual projects.

Immediately Get Started with Deployment and Smoke Testing of BigTop
===================================================================

The simplest way to get a feel for how bigtop works, is to just cd into `bigtop-deploy/vm` and try out the recipes under vagrant-puppet-vm, vagrant-puppet-docker, and so on.  Each one rapidly spins up, and runs the bigtop smoke tests on, a local bigtop based big data distribution.  Once you get the gist, you can hack around with the recipes to learn how the puppet/rpm/smoke-tests all work together, going deeper into the components you are interested in as described below.

Quick overview of source code directories
=========================================

* __bigtop-deploy__ : deployment scripts, puppet stuff, VM utilities for Apache Bigtop.
* __bigtop-packages__ : RPM/DEB specifications for Apache Bigtop subcomponents
* __bigtop-test-framework__ : The source code for the iTest utilities (framework used by smoke tests).
* __bigtop-tests__ : 
* __test-artifacts__ : source for tests.
* __test-execution__ : maven pom drivers for running the integration tests found in test-artifacts.
* __bigtop-toolchain__ : puppet scripts for setting up an instance which can build Apache Bigtop, sets up utils like jdk/maven/protobufs/...

Also, there is a new project underway, Apache Bigtop blueprints, which aims to create templates/examples that demonstrate/compare various Apache Hadoop ecosystem components with one another.

Contributing 
============

There are lots of ways to contribute.  People with different expertise can help with various subprojects:
    
* __puppet__ : Much of the Apache Bigtop deploy and packaging tools use puppet to bootstrap and set up a cluster. But recipes for other tools are also welcome (ie. Chef, Ansible, etc.)
* __groovy__ : Primary language used to write the Apache Bigtop smokes and itest framework. 
* __maven__ : Used to build Apache Bigtop smokes and also to define the high level Apache Bigtop project. 
* __RPM/DEB__ : Used to package Apache Hadoop ecosystem related projects into GNU/Linux installable packages for most popular GNU/Linux distributions. So one could add a new project or improve existing packages.
* __hadoop__ : Apache Hadoop users can also contribute by using the Apache Bigtop smokes, improving them, and evaluating their breadth.
* __contributing your workloads__ : Contributing your workloads enable us to tests projects against real use cases and enable you to have people verifying the use cases you care about are always working.
* __documentation__ : We are always in need of a better documentation!
* __giving feedback__ : Tell us how you use Apache Bigtop, what was great and what was not so great. Also, what are you expecting from it and what would you like to see in the future?
 
Also, opening [JIRA's](https://issues.apache.org/jira/browse/BIGTOP) and getting started by posting on the mailing list is helpful.

CTR model
=========

Bigtop supports Commit-Then-Review model of development. The following
rules will be used for the CTR process:
  * a committer can go ahead and commit the patch without mandatory review if
    felt confident in its quality (e.g. reasonable testing has been done
    locally; all compilations pass; RAT check is passed; the patch follows
    coding guidelines)
  * a committer is encouraged to seek peer-review and/or advice before hand if
    there're doubts in the approach taken, design decision, or implementation
    details
  * a committer should keep an eye on the official CI builds at
    https://ci.bigtop.apache.org/view/Packages/job/Bigtop-trunk-packages/ (Bigtop-trunk-packages builds)
    to make sure that committed changes haven't break anything. In
    which case the committer should take a timely effort to resolve the issues
    and unblock the others in the community
  * any non-document patch is required to be opened for at least 24 hours for
    community feedback before it gets committed unless it has an explicit +1
    from another committer
  * any non-document patch needs to address all the comment and reach consensus
    before it gets committed without a +1 from other committers
  * there's no changes in the JIRA process, except as specified above

What do people use Apache Bigtop for? 
==============================

You can go to the [Apache Bigtop website](http://bigtop.apache.org/) for notes on how to do "common" tasks like:

  * Apache Hadoop App developers: Download an Apache Bigtop built Apache Hadoop 2.0 VM from the website, so you can have a running psuedodistributed Apache Hadoop cluster to test your code on.
  * Cluster administers or deployment gurus: Run the Apache Bigtop smoke tests to ensure that your cluster is working.
  * Vendors: Build your own Apache Hadoop distribution, customized from Apache Bigtop bits.

Getting Started
===============

Below are some recipes for getting started with using Apache Bigtop. As Apache Bigtop has different subprojects, these recipes will continue to evolve.  
For specific questions it's always a good idea to ping the mailing list at dev-subscribe@bigtop.apache.org to get some immediate feedback, or [open a JIRA](https://issues.apache.org/jira/browse/BIGTOP).

For Users: Running the smoke tests.
-----------------------------------

The simplest way to test bigtop is described in bigtop-tests/smoke-tests/README file

For integration (API level) testing with maven, read on. 

For Users: Running the integration tests.
-----------------------------------------

WARNING: since testing packages requires installing them on a live system it is highly recommended to use VMs for that. Testing Apache Bigtop is done using iTest framework. The tests are organized in maven submodules, with one submodule per Apache Bigtop component.  The bigtop-tests/test-execution/smokes/pom.xml defines all submodules to be tested, and each submodule is in its own directory under smokes/, for example:
 
*smokes/hadoop/pom.xml*
*smokes/hive/pom.xml*
*... and so on.*

* New way (with Gradle build in place)
  * Step 1: install smoke tests for one or more components
    * Example 1:

        gradle installTestArtifacts

    * Example 2: Installing just Hadoop-specific smoke tests

        gradle install-hadoop

  * Step 2: Run the the smoke tests on your cluster (see Step 3 and/or Step 4 below)

  We are on the route of migrating subprojects under top-level gradle build. Currently
  converted projects could be listed by running

        gradle projects

  To see the list of tasks in a subproject, ie itest-common, you can run

        gradle itest-common:tasks

* Old Way
  * Step 1: Build the smokes with snapshots.  This ensures that all transitive dependencies etc.. are in your repo

        mvn clean install -DskipTests -DskipITs -DperformRelease -f ./bigtop-test-framework/pom.xml
        mvn clean install -DskipTests -DskipITs -DperformRelease -f ./test-artifacts/pom.xml

  * Step 2: Now, rebuild in "offline" mode.  This will make sure that your local changes to bigtop are embeded in the changes.
    
        mvn clean install -DskipTests -DskipITs -DperformRelease -o -nsu -f ./bigtop-test-framework/pom.xml
        mvn clean install -DskipTests -DskipITs -DperformRelease -o -nsu -f ./bigtop-tests/test-artifacts/pom.xml
 
  * Step 3: Now, you can run the smoke tests on your cluster.
    * Example 1: Running all the smoke tests with TRACE level logging (shows std out from each mr job). 
      
            mvn clean verify -Dorg.apache.bigtop.itest.log4j.level=TRACE -f ./bigtop/bigtop-tests/test-execution/smokes/pom.xml 

    * Just running hadoop examples, nothing else.

            mvn clean verify -D'org.apache.maven-failsafe-plugin.testInclude=**/*TestHadoopExamples*' -f bigtop-tests/test-execution/smokes/hadoop/pom.xml
 
    Note: A minor bug/issue: you need the "testInclude" regular expression above, even if you don't want to customize the tests, 
    since existing test names don't follow the maven integration test naming convention of IT*, but instead, follow the surefire (unit test) convention of Test*.

For Users: Creating Your Own Apache Hadoop Environment 
-----------------------------------------------

Another common use case for Apache Bigtop is creating / setting up your own Apache Hadoop distribution.  
For details on this, check out the bigtop-deploy/README.md file, which describes how to use the puppet repos
to create and setup your VMs.  
There is a current effort underway to create vagrant/docker recipes as well, which will be contained in the 
bigtop-deploy/ package.     


For Developers: Building the entire distribution from scratch
-------------------------------------------------------------
 
Packages have been built for CentOS/RHEL 5 and 6, Fedora 18, SuSE Linux Enterprise 11, OpenSUSE12.2, Ubuntu LTS Lucid and Precise, and Ubuntu Quantal. They can probably be built for other platforms as well. Some of the binary artifacts might be compatible with other closely related distributions.
 
__On all systems, Building Apache Bigtop requires certain set of tools__

  To bootstrap the development environment from scratch execute

    ./gradlew toolchain

  This build task expected Puppet 3.x to be installed; user has to have sudo permissions. The task will pull down and install
  all development dependencies, frameworks and SDKs, required to build the stack on your platform.

  To immediately set environment after running toolchain, run

    . /etc/profile.d/bigtop.sh

* __Building packages__ : `gradle [component-name]-[rpm|deb]`

  If -Dbuildwithdeps=true is set, the Gradle will follow the order of the build specified in
  the "dependencies" section of bigtop.bom file. Otherwise just a single component will get build (original behavior).

  To use an alternative definition of a stack composition (aka BOM), specify its
  name with -Dbomfile=<filename> system property in the build time.

  You can visualize all tasks dependencies by running `gradle tasks --all`
* __Building local YUM/APT repositories__ : `gradle [component-name]-[yum|apt]`

* __Recommended build environments__

  Bigtop provides "development in the can" environments, using Docker containers.
  These have the build tools set by the toolchain, as well as the user and build
  environment configured and cached. All currently supported OSes could be pulled
  from official Bigtop repository at https://hub.docker.com/r/bigtop/slaves/tags/

  To build a component (bigtop-groovy) for a particular OS (ubuntu-14.04) you can
  run the following from a clone of Bigtop workspace (assuming your system has
  Docker engine setup and working)
  ```docker run --rm -u jenkins:jenkins -v `pwd`:/ws --workdir /ws bigtop/slaves:trunk-ubuntu-14.04
  bash -l -c './gradlew allclean ; ./gradlew bigtop-groovy-pkg'```

For Developers: Building and modifying the web site
---------------------------------------------------

The website can be built by running `mvn site:site` from the root directory of the
project.  The main page can be accessed from "project_root/target/site/index.html".

The source for the website is located in "project_root/src/site/".


For Developers: Building a component from Git repository
--------------------------------------------------------

To fetch source from a Git repository you need to modify `bigtop.bom` and add the
following JSON snippets to your component/package:

```
git     { repo = ""; ref = ""; dir = ""}
```

* `repo` - SSH, HTTP or local path to Git repo.
* `ref` - branch, tag or commit hash to check out.
* `dir` - directory name to write source into.

Some packages have different names for source directory and source tarball
(`hbase-0.98.5-src.tar.gz` contains `hbase-0.98.5` directory).
By default source will be fetched in a directory named by `tarball { destination = TARBALL_DST}`
without `.t*` extension.
To explicitly set directory name use the `dir` option.

Example for HBase:

```
      name    = 'hbase'
      version { base = '1.1.3'; pkg = base; release = 1 }
      git     { repo = "https://github.com/apache/hbase.git"
                ref  = "${version.base}"
                dir  = "${name}-${version.base}" }
```


Contact us
----------

You can get in touch with us on [the Apache Bigtop mailing lists](http://bigtop.apache.org/mail-lists.html).

