BigPetStore -- Spark
====================

BigPetStore is a family of example applications for the Hadoop and Spark
ecosystems.  BigPetStore is build around a fictional chain pet stores,
providing generators for synthetic transaction data and pipelines for
processing that data.  Each ecosystems has its own version of the
application.

The Spark application currently builds against Spark 1.1.0.

Architecture
------------
The Spark application consists of the following modules so far:

* generator: generates raw data on the dfs

Building and Running with Spark
-------------------------------
BigPetStore has a Spark driver for generating data with the new data generator.
Build a fat jar as follows:

```
gradle clean shadowJar
```

This will produce a jar file under `build/libs` (referred to as `bigpetstore-spark-X.jar`).  You can then
use this jar to run a Spark job as follows:

```
spark-submit --master local[2] --class org.apache.bigtop.bigpetstore.generator.SparkDriver bigpetstore-spark-X.jar generated_data/ 10 1000 365.0 345
```

You will need to change the master if you want to run on a cluster.  The last five parameters control the output directory,
the number of stores, the number of customers, simulation length (in days), and the random seed (which is optional).

Running Tests
-------------
BigPetStore Spark includes unit tests that you can run with the following command:

```
gradle test
```
