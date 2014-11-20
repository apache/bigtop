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
* datamodel: data model used as input for analytics components
* etl: normalizes and transforms the raw data to the data model

Data Model
----------

The data generator creates a dirty CSV file containing the following fields:

* Store ID: Int
* Store Zipcode: String
* Store City: String
* Store State: String
* Customer ID: Int
* Customer First Name: String
* Customer Last Name: String
* Customer Zipcode: String
* Customer City: String
* Customer State: String
* Transaction ID: Int
* Transation Date Time: String (e.g., "Tue Nov 03 01:08:11 EST 2014")
* Transaction Product: String (e.g., "category=dry cat food;brand=Feisty Feline;flavor=Chicken & Rice;size=14.0;per_unit_cost=2.14;")

Note that the transaction ID is unique only per customer -- the customer and transaction IDs form a unique composite key.

Since the dirty CSV data contains repetitive information and requires massaging to use for analytics, an
internal structured data model is defined as input for the analytics components:

* Location(zipcode: String, city: String, state: String)
* Customer(customerId: Long, firstName: String, lastName: String, zipcode: String)
* Store(storeId: Long, zipcode: String)
* Product(productId: Long, category: String, attributes: Map[String, String])
* Transaction(customerId: Long, transactionId: Long, storeId: Long, dateTime: java.util.Calendar, productId: Long)

The ETL stage parses and cleans up the dirty CSV and writes out RDDs for each data type in the data model, serialized using
the `saveAsObjectFile()` method.  The analytics components can use the `IOUtils.load()` method to de-serialize the structured
data.

Running Tests
-------------
BigPetStore Spark includes unit tests that you can run with the following command:

```
gradle clean test
```

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
spark-submit --master local[2] --class org.apache.bigtop.bigpetstore.spark.generator.SparkDriver bigpetstore-spark-X.jar generated_data/ 10 1000 365.0 345
```

You will need to change the master if you want to run on a cluster.  The last five parameters control the output directory,
the number of stores, the number of customers, simulation length (in days), and the random seed (which is optional).


Running the ETL component
-------------------------
The data produced by the generator is in a raw text format, similar to what users will see in production environments.
The raw data isn't normalized (e.g., repeated customer, store, location, and product information) and needs to be parsed
(e.g., dates) before it can be easily used.  The ETL component does this for us.

The ETL component:

* Reads the raw data
* Parses the data times and products
* Normalizes the data
* Writes out RDDs for each type of class (Store, Customer, Location, Product, Transaction) in the data model

After building the jar (see above), you can run the ETL component like so:

```
spark-submit --master local[2] --class org.apache.bigtop.bigpetstore.spark.etl.SparkETL bigpetstore-spark-X.jar generated\_data transformed\_data
```
