QFS puppet module
=================
This module contains puppet recipes for various qfs components, e.g. metaserver,
chunkserver, and the webui. It is responsible for the following items:
    - Installing all required packages, configuration, init scripts, etc
    - Wiring everything up so that each service can talk to the other

hadoop-qfs
==========
Furthermore, this module also installs a compatibility wrapper script called
hadoop-qfs to make the hadoop and qfs integration and interaction easier.

In order to tell the main hadoop command to use qfs as the underlying
filesystem, extra options must be specified. For example, to issue a `hadoop fs`
command, the full command line would look like this:

    $ JAVA_LIBRARY_PATH=/usr/lib/qfs hadoop fs
        -Dfs.qfs.impl=com.quantcast.qfs.hadoop.QuantcastFileSystem \
        -Dfs.default.name=qfs://localhost:20000 \
        -Dfs.qfs.metaServerHost=localhost \
        -Dfs.qfs.metaServerPort=20000 \
        -ls /

This (a) is cumbersome and (b) exposes low level details, e.g. metaserver port
numbers, to the user who likely doesn't care. In order to avoid a poor user
experience, we provide a wrapper script around the main hadoop command called
`hadoop-qfs` which handles all the boilerplate for you. Using `hadoop-qfs`, the
command above is reduced to the following:

    $ hadoop-qfs fs -ls /

The `hadoop-qfs` command also supports submitting jobs to hadoop which will use
qfs as the underlying filesystem instead of the default HDFS. The process is the
exact same as the standard `hadoop` command, except with using `hadoop-qfs`.

    $ hadoop-qfs jar hadoop-mapreduce-examples.jar pi 100 100

Any output data the job writes will be stored in qfs instead of HDFS. Note that
when submitting jobs through the `hadoop-qfs` command, both the path to the jar
file as well as the main class are required options.

See the usage for `hadoop-qfs` (`hadoop-qfs --help`) for more information.

Python
======
To use the qfs bindings in python, you must set the LD_LIBRARY_PATH to where the
qfs shared objects are stored so that they can be found.

    [root@bigtop1 /]# LD_LIBRARY_PATH=/usr/lib/qfs python
    Python 2.6.6 (r266:84292, Jul 23 2015, 15:22:56)
    [GCC 4.4.7 20120313 (Red Hat 4.4.7-11)] on linux2
    Type "help", "copyright", "credits" or "license" for more information.
    >>> import qfs
    >>>

Why Not Update Hadoop's `core-site.xml`?
========================================
The following is a little discussion on the rationale behind why this script has
been implemented the way it has and some of the other options considered.

One option is to include these configuration options in the core-site.xml file
in a hadoop configuration directory. If we modify the core-site.xml file that
the hadoop component installs, qfs silently takes precedence over hdfs. This
results in a bad user experience since users should have a clear way to choose
which filesystem they want to interact with.

Another option is to provide our own core-site.xml specific to qfs in an
alternate location and use the --config option to the hadoop command. This works
but all of the other configuration in place for hadoop will either be missing or
have to be symlinked in order to be carried over since we are overriding *all*
configuration. This is annoying and brittle -- new configuration will have to be
carried over whenever it is added.  Having two places where configuration needs
to live is usually a bad idea.

The final option considered, and the one used, is to provide a wrapper around
the hadoop command, don't touch any hadoop configuration, and set the necessary
override parameters on the command line here. This seemed the cleanest solution
and also made sure that a qfs installation is as non-confrontational as
possible. Unfortunately, the hadoop command is very picky about where the
parameter overrides go depending on the subcommand used.
