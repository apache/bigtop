![Bigtop](https://github.com/cloudera/bigtop/blob/master/docs/logo.jpg?raw=true)

# Welcome to Bigtop!

Bigtop is a project for the development of packaging and tests of the [Apache Hadoop](http://hadoop.apache.org/) ecosystem.

The primary goal of Bigtop is to build a community around the packaging and interoperability testing of Hadoop-related projects. This includes testing at various levels (packaging, platform, runtime, upgrade, etc...) developed by a community with a focus on the system as a whole, rather than individual projects.

## Building Bigtop

Packages have been built on Ubuntu 10.10, CentOS 5 and openSUSE 11.4. They can probably be built on other platforms as well.

Building Bigtop requires the following tools:

* Java JDK 1.6
* Apache Forrest 0.8 (requires 32bit version of Java JDK 1.5)
* Apache Ant
* Apache Maven
* git
* subversion
* autoconf
* automake
* liblzo2-dev
* libz-dev
* sharutils
* libfuse-dev

On Debian-based systems one also needs

* build-essential dh-make debhelper devscripts
* reprepro

## Building packages

    $ make [component-name]-[rpm|deb]

## Building local YUM/APT repositories

    $ make [component-name]-[yum|apt]

##  Running the tests

WARNING: since testing packages requires installing them on a live system it is highly recommended to use VMs for that.

Testing Bigtop is done using iTest framework. For more documentation on iTest visit the [iTest page](http://cloudera.github.com/bigtop/iTest) but here's 2 steps to get started:

* install package testing iTest artifacts locally:

        cd test/src/smokes/package/ && mvn install -DskipTests -DskipITs -DperformRelease

* use those locally installed iTest package testing artifacts to run a suite:

        cd test/suites/package/ && mvn clean verify -Dcdh.repo.file.url.CentOS=XXX  -D'org.apache.maven-failsafe-plugin.testInclude=**/TestPackagesReadiness.*'

##  Contact us!

You can get in touch with us on the [user list](https://groups.google.com/a/cloudera.org/group/bigtop-user/topics) or [developer list](https://groups.google.com/a/cloudera.org/group/bigtop-dev/topics).
