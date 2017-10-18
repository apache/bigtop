<!--
  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->
# Overview

Apache Spark is a fast and general purpose engine for large-scale data
processing. Learn more at [spark.apache.org][].

This charm deploys version 2.1.1 of the Spark component from [Apache Bigtop][].

[spark.apache.org]: http://spark.apache.org/
[Apache Bigtop]: http://bigtop.apache.org/


# Deploying

This charm requires Juju 2.0 or greater. If Juju is not yet set up, please
follow the [getting-started][] instructions prior to deploying this charm.

This charm supports running Spark in a variety of modes:

### Standalone
In this mode, Spark units form a cluster that can be scaled as needed.
Starting with a single node:

    juju deploy spark

Scale the cluster by adding more spark units:

    juju add-unit spark

When in standalone mode, Juju ensures a single Spark master is appointed.
The status of the unit acting as master reads `ready (standalone - master)`,
while the rest of the units display a status of `ready (standalone)`.
If the master is removed, Juju will appoint a new one. However, if a master
fails in standalone mode, running jobs and job history will be lost.

### Standalone HA
To enable High Availability for a Spark cluster, simply add Zookeeper to
the deployment. To ensure a Zookeeper quorum, 3 units of the zookeeper
application are recommended. For instance:

    juju deploy zookeeper -n 3
    juju add-relation spark zookeeper

In this mode, the cluster can again be scaled as needed by adding/removing
units. Spark units report `ready (standalone HA)` in their status. To identify
the unit acting as master, query Zookeeper as follows:

    juju run --unit zookeeper/0 'echo "get /spark/master_status" | /usr/lib/zookeeper/bin/zkCli.sh'

### YARN
This charm leverages our pluggable Hadoop model with the `hadoop-plugin`
interface. This means that this charm can be related to an Apache Hadoop
cluster to run Spark jobs there. The suggested deployment method is to use the
[hadoop-spark][] bundle:

    juju deploy hadoop-spark

To switch among the above execution modes, set the `spark_execution_mode`
config variable:

    juju config spark spark_execution_mode=<new_mode>

See the **Configuring** section below for supported mode options.

## Network-Restricted Environments
Charms can be deployed in environments with limited network access. To deploy
in this environment, configure a Juju model with appropriate proxy and/or
mirror options. See [Configuring Models][] for more information.

[getting-started]: https://jujucharms.com/docs/stable/getting-started
[hadoop-spark]: https://jujucharms.com/hadoop-spark/
[Configuring Models]: https://jujucharms.com/docs/stable/models-config


# Verifying

## Status
Apache Bigtop charms provide extended status reporting to indicate when they
are ready:

    juju status

This is particularly useful when combined with `watch` to track the on-going
progress of the deployment:

    watch -n 2 juju status

The message column will provide information about a given unit's state.
This charm is ready for use once the status message indicates that it is
ready.

## Smoke Test
This charm provides a `smoke-test` action that can be used to verify the
application is functioning as expected. Run the action as follows:

    juju run-action spark/0 smoke-test

Watch the progress of the smoke test actions with:

    watch -n 2 juju show-action-status

Eventually, the action should settle to `status: completed`.  If it
reports `status: failed`, the application is not working as expected. Get
more information about a specific smoke test with:

    juju show-action-output <action-id>

## Spark Master web UI
Spark provides a web console that can be used to verify information about
the cluster. To access it, find the `Public address` of the spark application
and expose it:

    juju status spark
    juju expose spark

The web interface will be available at the following URL:

    http://SPARK_PUBLIC_IP:8080

## Spark Job History web UI
The Job History server shows all active and finished spark jobs submitted.
As mentioned above, expose the spark application and note the public IP
address. The job history web interface will be available at the following URL:

    http://SPARK_PUBLIC_IP:18080


# Using

## Actions
Once Spark is ready, there are a number of actions available in this charm.

Run a benchmark (as described in the **Benchmarking** section):

    juju run-action spark/0 pagerank
    juju show-action-output <id>  # <-- id from above command

Run a smoke test (as described in the **Verifying** section):

    juju run-action spark/0 smoke-test
    juju show-action-output <id>  # <-- id from above command

Start/Stop/Restart the Spark Job History service:

    juju run-action spark/0 [start|stop|restart]-spark-job-history-server
    juju show-action-output <id>  # <-- id from above command

Submit a Spark job:

    juju run-action spark/0 spark-submit \
      options='--class org.apache.spark.examples.SparkPi' \
      job='/usr/lib/spark/examples/jars/spark-examples.jar' \
      job-args='10'
    juju show-action-output <id>  # <-- id from above command

## Spark shell
Spark shell provides a simple way to learn the API, as well as a powerful
tool to analyze data interactively. It is available in either Scala or Python
and can be run from the Spark unit as follows:

    juju ssh spark/0
    spark-shell # for interaction using scala
    pyspark     # for interaction using python

## Command line
SSH to the Spark unit and manually run a spark-submit job, for example:

    juju ssh spark/0
    spark-submit --class org.apache.spark.examples.SparkPi \
     /usr/lib/spark/examples/jars/spark-examples.jar 10

## Apache Zeppelin
Apache Zeppelin is a web-based notebook that enables interactive data
analytics. Make beautiful data-driven, interactive, and collaborative documents
with SQL, Scala and more. Deploy Zeppelin and relate it to Spark:

    juju deploy zeppelin
    juju add-relation spark zeppelin

To access the web console, find the `Public address` of the zeppelin
application and expose it:

    juju status zeppelin
    juju expose zeppelin

The web interface will be available at the following URL:

    http://ZEPPELIN_PUBLIC_IP:9080


# Configuring

Charm configuration can be changed at runtime with `juju config`. This charm
supports the following config parameters.

## driver_memory
Amount of memory available for the Spark driver process (1g by default).
Set a different value with:

    juju config spark driver_memory=4096m

## executor_memory
Amount of memory available for each Spark executor process (1g by default).
Set a different value with:

    juju config spark executor_memory=2g

> **Note**: When Spark is in YARN mode, ensure the configured executor memory
does not exceed the NodeManager maximum (defined on each nodemanager as
`yarn.nodemanager.resource.memory-mb` in `yarn-default.xml`).

## install-cuda
Provided by `layer-nvidia-cuda`, this option controls the installation
of NVIDIA CUDA packages if capable GPU hardware is present. When `false` (the
default), CUDA will not be installed or configured regardless of hardware
support. Set this to `true` to fetch and install CUDA-related packages from
the NVIDIA developer repository.

    juju config spark install-cuda=true

> **Note**: This option requires external network access to
http://developer.download.nvidia.com/. Ensure appropriate proxies are
configured if needed.

## spark_bench_enabled
Controls the installation of the [Spark-Bench][] benchmarking suite. When set
to `true`, this charm will download and install Spark-Bench from the URL
specified by the `spark_bench_url` config option. When set to `false`
(the default), Spark-Bench will not be installed on the unit, though any data
stored in `hdfs:///user/ubuntu/spark-bench` from previous installations will
be preserved.

> **Note**: Spark-Bench has not been verified to work with Spark 2.1.x.

> **Note**: This option requires external network access to the configured
Spark-Bench URL. Ensure appropriate proxies are configured if needed.

[Spark-Bench]: https://github.com/SparkTC/spark-bench

## spark_execution_mode
Spark has four modes of execution: local, standalone, yarn-client, and
yarn-cluster. The default mode is `standalone` and can be changed by setting
the `spark_execution_mode` config option.

  * **Local**

    In Local mode, Spark processes jobs locally without any cluster resources.
    There are 3 ways to specify 'local' mode:

    * `local`

      Run Spark locally with one worker thread (i.e. no parallelism at all).

    * `local[K]`

      Run Spark locally with K worker threads (ideally, set this to the number
      of cores on the deployed machine).

    * `local[*]`

      Run Spark locally with as many worker threads as logical cores on the
      deployed machine.

  * **Standalone**

    In `standalone` mode, Spark launches a Master and Worker daemon on the Spark
    unit. This mode is useful for simulating a distributed cluster environment
    without actually setting up a cluster.

  * **YARN-client**

    In `yarn-client` mode, the Spark driver runs in the client process, and the
    application master is only used for requesting resources from YARN.

  * **YARN-cluster**

    In `yarn-cluster` mode, the Spark driver runs inside an application master
    process which is managed by YARN on the cluster, and the client can go away
    after initiating the application.


# Benchmarking

This charm provides benchmarks to gauge the performance of the Spark cluster.
Each benchmark is an action that can be run with `juju run-action`:

    $ juju actions spark
    ...
    pagerank                          Calculate PageRank for a sample data set
    sparkpi                           Calculate Pi
    ...

    $ juju run-action spark/0 pagerank
    Action queued with id: 339cec1f-e903-4ee7-85ca-876fb0c3d28e

    $ juju show-action-output 339cec1f-e903-4ee7-85ca-876fb0c3d28e
    results:
      meta:
        composite:
          direction: asc
          units: secs
          value: "83"
        start: 2017-04-12T23:22:38Z
        stop: 2017-04-12T23:24:01Z
      output: '{''status'': ''completed''}'
    status: completed
    timing:
      completed: 2017-04-12 23:24:02 +0000 UTC
      enqueued: 2017-04-12 23:22:36 +0000 UTC
      started: 2017-04-12 23:22:37 +0000 UTC


# Issues

Apache Bigtop tracks issues using JIRA (Apache account required). File an
issue for this charm at:

https://issues.apache.org/jira/secure/CreateIssue!default.jspa

Ensure `Bigtop` is selected as the project. Typically, charm issues are filed
in the `deployment` component with the latest stable release selected as the
affected version. Any uncertain fields may be left blank.


# Contact Information

- <bigdata@lists.ubuntu.com>


# Resources

- [Apache Spark home page](http://spark.apache.org/)
- [Apache Bigtop home page](http://bigtop.apache.org/)
- [Apache Bigtop issue tracking](http://bigtop.apache.org/issue-tracking.html)
- [Apache Bigtop mailing lists](http://bigtop.apache.org/mail-lists.html)
- [Juju Big Data](https://jujucharms.com/big-data)
- [Juju Bigtop charms](https://jujucharms.com/q/bigtop)
- [Juju mailing list](https://lists.ubuntu.com/mailman/listinfo/juju)
