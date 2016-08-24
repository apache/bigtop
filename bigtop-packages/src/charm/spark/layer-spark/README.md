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
## Overview

Apache Spark™ is a fast and general purpose engine for large-scale data
processing. Key features:

 * **Speed**

 Run programs up to 100x faster than Hadoop MapReduce in memory, or 10x faster
 on disk. Spark has an advanced DAG execution engine that supports cyclic data
 flow and in-memory computing.

 * **Ease of Use**

 Write applications quickly in Java, Scala or Python. Spark offers over 80
 high-level operators that make it easy to build parallel apps, and you can use
 it interactively from the Scala and Python shells.

 * **General Purpose Engine**

 Combine SQL, streaming, and complex analytics. Spark powers a stack of
 high-level tools including Shark for SQL, MLlib for machine learning, GraphX,
 and Spark Streaming. You can combine these frameworks seamlessly in the same
 application.


## Deployment

This charm deploys the Spark component of the Apache Bigtop platform and
supports running Spark in a variety of modes:

 * **Standalone**

 In this mode Spark units form a cluster that you can scale to match your needs.
 Starting with a single node:

    juju deploy spark
    juju deploy openjdk
    juju add-relation spark openjdk

 You can scale the cluster by adding more spark units:

    juju add-unit spark

 When in standalone mode, Juju ensures a single Spark master is appointed.
 The status of the unit acting as master reads "ready (standalone - master)",
 while the rest of the units display a status of "ready (standalone)".
 If you remove the master, Juju will appoint a new one. However, if a master
 fails in standalone mode, running jobs and job history will be lost.

 * **Standalone HA**

 To enable High Availability for a Spark cluster, you need to add Zookeeper to
 the deployment. To ensure a Zookeeper quorum, it is recommended that you
 deploy 3 units of the zookeeper application. For instance:

    juju deploy apache-zookeeper zookeeper -n 3
    juju add-relation spark zookeeper

 In this mode, you can again scale your cluster to match your needs by
 adding/removing units. Spark units report "ready (standalone HA)" in their
 status. If you need to identify the node acting as master, query Zookeeper
 as follows:

    juju run --unit zookeeper/0 'echo "get /spark/master_status" | /usr/lib/zookeeper/bin/zkCli.sh'

 * **Yarn-client and Yarn-cluster**

 This charm leverages our pluggable Hadoop model with the `hadoop-plugin`
 interface. This means that you can relate this charm to a base Apache Hadoop cluster
 to run Spark jobs there. The suggested deployment method is to use the
 [hadoop-processing](https://jujucharms.com/hadoop-processing/)
 bundle and add a relation between spark and the plugin:

    juju deploy hadoop-processing
    juju add-relation plugin spark


Note: To switch to a different execution mode, set the
`spark_execution_mode` config variable:

    juju set spark spark_execution_mode=<new_mode>

See the **Configuration** section below for supported mode options.


## Usage

Once deployment is complete, you can manually load and run Spark batch or
streaming jobs in a variety of ways:

  * **Spark shell**

Spark’s shell provides a simple way to learn the API, as well as a powerful
tool to analyse data interactively. It is available in either Scala or Python
and can be run from the Spark unit as follows:

       juju ssh spark/0
       spark-shell # for interaction using scala
       pyspark     # for interaction using python

  * **Command line**

SSH to the Spark unit and manually run a spark-submit job, for example:

       juju ssh spark/0
       spark-submit --class org.apache.spark.examples.SparkPi \
        --master yarn-client /usr/lib/spark/lib/spark-examples*.jar 10

  * **Apache Zeppelin visual service**

Deploy Apache Zeppelin and relate it to the Spark unit:

    juju deploy apache-zeppelin zeppelin
    juju add-relation spark zeppelin

Once the relation has been made, access the web interface at
`http://{spark_unit_ip_address}:9090`

  * **IPyNotebook for Spark**

The IPython Notebook is an interactive computational environment, in which you
can combine code execution, rich text, mathematics, plots and rich media.
Deploy IPython Notebook for Spark and relate it to the Spark unit:

    juju deploy apache-spark-notebook notebook
    juju add-relation spark notebook

Once the relation has been made, access the web interface at
`http://{spark_unit_ip_address}:8880`


## Configuration

### `spark_bench_enabled`

Install the SparkBench benchmarking suite. If `true` (the default), this charm
will download spark bench from the URL specified by `spark_bench_ppc64le`
or `spark_bench_x86_64`, depending on the unit's architecture.

### `spark_execution_mode`

Spark has four modes of execution: local, standalone, yarn-client, and
yarn-cluster. The default mode is `yarn-client` and can be changed by setting
the `spark_execution_mode` config variable.

  * **Local**

    In Local mode, Spark processes jobs locally without any cluster resources.
    There are 3 ways to specify 'local' mode:

    * `local`

      Run Spark locally with one worker thread (i.e. no parallelism at all).

    * `local[K]`

      Run Spark locally with K worker threads (ideally, set this to the number
      of cores on your machine).

    * `local[*]`

      Run Spark locally with as many worker threads as logical cores on your
      machine.

  * **Standalone**

    In `standalone` mode, Spark launches a Master and Worker daemon on the Spark
    unit. This mode is useful for simulating a distributed cluster environment
    without actually setting up a cluster.

  * **YARN-client**

    In `yarn-client` mode, the driver runs in the client process, and the
    application master is only used for requesting resources from YARN.

  * **YARN-cluster**

    In `yarn-cluster` mode, the Spark driver runs inside an application master
    process which is managed by YARN on the cluster, and the client can go away
    after initiating the application.


## Verify the deployment

### Status and Smoke Test

The services provide extended status reporting to indicate when they are ready:

    juju status --format=tabular

This is particularly useful when combined with `watch` to track the on-going
progress of the deployment:

    watch -n 0.5 juju status --format=tabular

The message for each unit will provide information about that unit's state.
Once they all indicate that they are ready, you can perform a "smoke test"
to verify that Spark is working as expected using the built-in `smoke-test`
action:

    juju run-action spark/0 smoke-test

_**Note**: The above assumes Juju 2.0 or greater. If using an earlier version
of Juju, the syntax is `juju action do spark/0 smoke-test`._


After a minute or so, you can check the results of the smoke test:

    juju show-action-status

_**Note**: The above assumes Juju 2.0 or greater. If using an earlier version
of Juju, the syntax is `juju action status`._

You will see `status: completed` if the smoke test was successful, or
`status: failed` if it was not.  You can get more information on why it failed
via:

    juju show-action-output <action-id>

_**Note**: The above assumes Juju 2.0 or greater. If using an earlier version
of Juju, the syntax is `juju action fetch <action-id>`._


### Verify Job History

The Job History server shows all active and finished spark jobs submitted.
To view the Job History server you need to expose spark (`juju expose spark`)
and navigate to `http://{spark_master_unit_ip_address}:18080` of the
unit acting as master.


## Benchmarking

This charm provides several benchmarks, including the
[Spark Bench](https://github.com/SparkTC/spark-bench) benchmarking
suite (if enabled), to gauge the performance of your environment.

The easiest way to run the benchmarks on this service is to relate it to the
[Benchmark GUI][].  You will likely also want to relate it to the
[Benchmark Collector][] to have machine-level information collected during the
benchmark, for a more complete picture of how the machine performed.

[Benchmark GUI]: https://jujucharms.com/benchmark-gui/
[Benchmark Collector]: https://jujucharms.com/benchmark-collector/

However, each benchmark is also an action that can be called manually:

    $ juju action do spark/0 pagerank
    Action queued with id: 88de9367-45a8-4a4b-835b-7660f467a45e
    $ juju action fetch --wait 0 88de9367-45a8-4a4b-835b-7660f467a45e
    results:
      meta:
        composite:
          direction: asc
          units: secs
          value: "77.939000"
        raw: |
          PageRank,2015-12-10-23:41:57,77.939000,71.888079,.922363,0,PageRank-MLlibConfig,,,,,10,12,,200000,4.0,1.3,0.15
        start: 2015-12-10T23:41:34Z
        stop: 2015-12-10T23:43:16Z
      results:
        duration:
          direction: asc
          units: secs
          value: "77.939000"
        throughput:
          direction: desc
          units: x/sec
          value: ".922363"
    status: completed
    timing:
      completed: 2015-12-10 23:43:59 +0000 UTC
      enqueued: 2015-12-10 23:42:10 +0000 UTC
      started: 2015-12-10 23:42:15 +0000 UTC

Valid action names at this time are:

  * logisticregression
  * matrixfactorization
  * pagerank
  * sql
  * streaming
  * svdplusplus
  * svm
  * trianglecount
  * sparkpi


## Contact Information

- <bigdata@lists.ubuntu.com>


## Help

- [Juju mailing list](https://lists.ubuntu.com/mailman/listinfo/juju)
- [Juju community](https://jujucharms.com/community)
