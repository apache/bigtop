(See accompanying source code for licensing information)

BigPetStore
============

Apache Bigtop/Hadoop Ecosystem Demo
-----------------------------------
This software is created to demonstrate Apache Bigtop for processing
big data sets.

Architecture
------------
The application consists of the following modules

* generator: generates raw data on the dfs
* clustering: Apache Mahout demo code for processing the data using Item based Collaborative Filtering. This feature is not supported yet. You can track its progress using this [`JIRA` issue](https://issues.apache.org/jira/browse/BIGTOP-1272)
* Pig: demo code for processing the data using Apache Pig
* Hive: demo code for processing the data using Apache Hive. This part is not complete yet. We are working on it. You can track it using this [`JIRA` issue](https://issues.apache.org/jira/browse/BIGTOP-1270)
* Crunch: demo code for processing the data using Apache Crunch

Build Instructions
------------------

You'll need to have [`gradle`](http://www.gradle.org/downloads) installed and set-up correctly in order to follow along these instructions.
We could have used the [`gradle-wrapper`](http://www.gradle.org/docs/current/userguide/gradle_wrapper.html) to avoid having to install `gradle`, but the `bigtop` project includes all `gradle*` directories in `.gitignore`. So, that's not going to work.

### Build the JAR

  `gradle clean build` will build the bigpetstore `jar`. The `jar` will be located in the `build\libs` directory.

### Run Intergration Tests With
  * Pig profile: `gradle clean integrationTest -P ITProfile=pig`
  * Crunch profile: `gradle clean integrationTest -P ITProfile=crunch`
  * Hive profile: Not implemented yet.
  * Mahout profile: Not implemented yet.

If you don't specify any profile-name, or if you specify an invalid-name for the `integrationTest` task, no integration tests will be run.

*Note:* At this stage, only the `Pig` profile is working. Will continue to update this area as further work is completed.

For Eclipse Users
-----------------

1. Run `gradle eclipse` to create an eclipse project.
2. Import the project into eclipse.

*Note* whenever you modify the dependencies, you will need to run the `gradle eclipse` again. Refresh the project after doing so. You'd also need to have the `scala` plugin installed. Also, having a `gradle` plugin would be quite useful as well, for ex. when you want to update dependencies.

High level summary
------------------

The bigpetstore project exemplifies the hadoop ecosystem for newcomers, and also for benchmarking and
comparing functional space of tools.

The end goal is to run many different implementations of each phase
using different tools, thus exemplifying overlap of tools in the hadoop ecosystem, and allowing people to benchmark/compare tools
using a common framework and easily understood use case


How it works (To Do)
--------------------

### Phase 1: Generating pet store data:

The first step is to generate a raw data set.  This is done by the "GeneratePetStoreTransactionsInputFormat":

The first MapReduce job in the pipeline runs a simple job which takes this input format and forwards
its output.  The result is a list of "transactions".  Each transaction is a tuple of the format

  *{state,name,date,price,product}.*

### Phase 2: Processing the data

The next phase of the application processes the data to create basic aggregations. For example with both pig and hive these could easily include

- *Number of transactions by state* or
- *Most valuable customer by state* or
- *Most popular items by state*


### Phase 3: Clustering the states by all fields

  Now, say we want to cluster the states, so as to put different states into different buying categories
  for our marketing team to deal with differently.

### Phase 4: Visualizing the Data in D3.

 - try it [on the gh-pages branch](http://jayunit100.github.io/bigpetstore/)

Running on a hadoop cluster
---------------------------

wget s3://bigpetstore/bigpetstore.jar

hadoop jar bigpetstore.jar org.apache.bigtop.bigpetstore.generator.BPSGenerator 1000000 bigpetstore/gen

hadoop jar bigpetstore.jar org.apache.bigtop.bigpetstore.etl.PigCSVCleaner bigpetstore/gen/ bigpetstore/pig/ custom_pigscript.pig
... (will add more steps as we add more phases to the workflow) ...


Example of running in EMR
--------------------------
- Put the jar in s3.  Right now there is a copy of it at the url below.

- Download the elastic-mapreduce ruby shell script.
create your "credentials.json" file.

Now run this to generate 1,000,000 pet store transactions:

./elastic-mapreduce --create --jar s3://bigpetstore/bigpetstore.jar \
--main-class org.apache.bigtop.bigpetstore.generator.BPSGenerator \
--num-instances 10  \
--arg 1000000 \
--arg s3://bigpetstore/data/generated \
--hadoop-version "2.2.0"  \
--master-instance-type m1.medium \
--slave-instance-type m1.medium

...Now lets clean the data with pig...

Replace the above "main-class", and "--arg" options with
--main-class org.apache.bigtop.bigpetstore.etl.PigCSVCleaner
--arg s3://bigpetstore/data/generated
--arg s3://bigpetstore/data/pig_out
(optional, you can send a script referencing the cleaned $input path to do some
custom analytics, see the BPS_Analytics.pig script and companion
http://jayunit100.github.io/bigpetstore) as an example).
--arg s3://path_to_custom_analytics_script.pig

(note about pig: We support custom pig scripts.... for EMR, custom pig scripts will need to point to a
local path, so youll have to put that script on the machine as part
of EMR setup w/ a custom script).

...

And so on.
