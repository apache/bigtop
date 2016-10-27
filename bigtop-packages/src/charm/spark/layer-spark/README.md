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
processing. This charm deploys the Spark component of the [Apache Bigtop][]
platform. Key features:

 * **Speed**

 Run programs up to 100x faster than Hadoop MapReduce in memory, or 10x faster
 on disk. Spark has an advanced DAG execution engine that supports cyclic data
 flow and in-memory computing.

 * **Ease of Use**

 Write applications quickly in Java, Scala or Python. Spark offers over 80
 high-level operators that make it easy to build parallel apps for use
 interactively from the Scala and Python shells.

 * **General Purpose Engine**

 Combine SQL, streaming, and complex analytics. Spark powers a stack of
 high-level tools including Shark for SQL, MLlib for machine learning, GraphX,
 and Spark Streaming. Combine these frameworks seamlessly in the same
 application.

[Apache Bigtop]: http://bigtop.apache.org/


# Deploying

A working Juju installation is assumed to be present. If Juju is not yet set
up, please follow the [getting-started][] instructions prior to deploying this
charm.

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

### YARN-client and YARN-cluster
This charm leverages our pluggable Hadoop model with the `hadoop-plugin`
interface. This means that this charm can be related to a base Apache Hadoop
cluster to run Spark jobs there. The suggested deployment method is to use the
[hadoop-processing][] bundle and add a relation between spark and the plugin.


    juju deploy hadoop-processing
    juju add-relation plugin spark

> **Note**: The above assumes Juju 2.0 or greater. If using an earlier version
of Juju, use [juju-quickstart][] with the following syntax: `juju quickstart
hadoop-processing`.

To switch among the above execution modes, set the `spark_execution_mode`
config variable:

    juju config spark spark_execution_mode=<new_mode>

> **Note**: The above assumes Juju 2.0 or greater. If using an earlier version
of Juju, the syntax is `juju set spark spark_execution_mode=<new_mode>`.

See the **Configuring** section below for supported mode options.

## Network-Restricted Environments
Charms can be deployed in environments with limited network access. To deploy
in this environment, configure a Juju model with appropriate proxy and/or
mirror options. See [Configuring Models][] for more information.

[getting-started]: https://jujucharms.com/docs/stable/getting-started
[hadoop-processing]: https://jujucharms.com/hadoop-processing/
[juju-quickstart]: https://launchpad.net/juju-quickstart
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

> **Note**: The above assumes Juju 2.0 or greater. If using an earlier version
of Juju, the syntax is `juju action do spark/0 smoke-test`.

Watch the progress of the smoke test actions with:

    watch -n 2 juju show-action-status

> **Note**: The above assumes Juju 2.0 or greater. If using an earlier version
of Juju, the syntax is `juju action status`.

Eventually, the action should settle to `status: completed`.  If it
reports `status: failed`, the application is not working as expected. Get
more information about a specific smoke test with:

    juju show-action-output <action-id>

> **Note**: The above assumes Juju 2.0 or greater. If using an earlier version
of Juju, the syntax is `juju action fetch <action-id>`.

## Spark Master web UI
Spark provides a web console that can be used to verify information about
the cluster. To access it, find the `PUBLIC-ADDRESS` of the spark application
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

Once deployment is verified, Spark batch or streaming jobs can be run in a
variety of ways:

### Spark shell
Spark shell provides a simple way to learn the API, as well as a powerful
tool to analyze data interactively. It is available in either Scala or Python
and can be run from the Spark unit as follows:

    juju ssh spark/0
    spark-shell # for interaction using scala
    pyspark     # for interaction using python

### Command line
SSH to the Spark unit and manually run a spark-submit job, for example:

    juju ssh spark/0
    spark-submit --class org.apache.spark.examples.SparkPi \
     --master yarn-client /usr/lib/spark/lib/spark-examples*.jar 10

### Apache Zeppelin
Apache Zeppelin is a web-based notebook that enables interactive data
analytics. Make beautiful data-driven, interactive, and collaborative documents
with SQL, Scala and more. Deploy Zeppelin and relate it to Spark:

    juju deploy zeppelin
    juju add-relation spark zeppelin

To access the web console, find the `PUBLIC-ADDRESS` of the zeppelin
application and expose it:

    juju status zeppelin
    juju expose zeppelin

The web interface will be available at the following URL:

    http://ZEPPELIN_PUBLIC_IP:9080


# Configuring

## spark_bench_enabled

Install the SparkBench benchmarking suite. If `true` (the default), this charm
will download spark bench from the URL specified by `spark_bench_ppc64le`
or `spark_bench_x86_64`, depending on the unit's architecture.

## spark_execution_mode

Spark has four modes of execution: local, standalone, yarn-client, and
yarn-cluster. The default mode is `standalone` and can be changed by setting
the `spark_execution_mode` config variable.

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

This charm provides several benchmarks, including the [Spark Bench][]
benchmarking suite (if enabled), to gauge the performance of the environment.
Each benchmark is an action that can be run with `juju run-action`:

    $ juju actions spark | grep Bench
    connectedcomponent                Run the Spark Bench ConnectedComponent benchmark.
    decisiontree                      Run the Spark Bench DecisionTree benchmark.
    kmeans                            Run the Spark Bench KMeans benchmark.
    linearregression                  Run the Spark Bench LinearRegression benchmark.
    logisticregression                Run the Spark Bench LogisticRegression benchmark.
    matrixfactorization               Run the Spark Bench MatrixFactorization benchmark.
    pagerank                          Run the Spark Bench PageRank benchmark.
    pca                               Run the Spark Bench PCA benchmark.
    pregeloperation                   Run the Spark Bench PregelOperation benchmark.
    shortestpaths                     Run the Spark Bench ShortestPaths benchmark.
    sql                               Run the Spark Bench SQL benchmark.
    stronglyconnectedcomponent        Run the Spark Bench StronglyConnectedComponent benchmark.
    svdplusplus                       Run the Spark Bench SVDPlusPlus benchmark.
    svm                               Run the Spark Bench SVM benchmark.

    $ juju run-action spark/0 svdplusplus
    Action queued with id: 339cec1f-e903-4ee7-85ca-876fb0c3d28e

    $ juju show-action-output 339cec1f-e903-4ee7-85ca-876fb0c3d28e
    results:
      meta:
        composite:
          direction: asc
          units: secs
          value: "200.754000"
        raw: |
          SVDPlusPlus,2016-11-02-03:08:26,200.754000,85.974071,.428255,0,SVDPlusPlus-MLlibConfig,,,,,10,,,50000,4.0,1.3,
        start: 2016-11-02T03:08:26Z
        stop: 2016-11-02T03:11:47Z
      results:
        duration:
          direction: asc
          units: secs
          value: "200.754000"
        throughput:
          direction: desc
          units: x/sec
          value: ".428255"
    status: completed
    timing:
      completed: 2016-11-02 03:11:48 +0000 UTC
      enqueued: 2016-11-02 03:08:21 +0000 UTC
      started: 2016-11-02 03:08:26 +0000 UTC

[Spark Bench]: https://github.com/SparkTC/spark-bench


# Contact Information

- <bigdata@lists.ubuntu.com>


# Resources

- [Apache Bigtop](http://bigtop.apache.org/) home page
- [Apache Bigtop issue tracking](http://bigtop.apache.org/issue-tracking.html)
- [Apache Bigtop mailing lists](http://bigtop.apache.org/mail-lists.html)
- [Juju Bigtop charms](https://jujucharms.com/q/apache/bigtop)
- [Juju mailing list](https://lists.ubuntu.com/mailman/listinfo/juju)
- [Juju community](https://jujucharms.com/community)
