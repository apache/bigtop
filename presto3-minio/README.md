# Setting up a data warehouse with Minio and Presto

The test.sh script in this module demonstrates how to deploy minio alongside presto such that you
can then use Minio as a Catalog inside of presto.

After deployment of the presto coordinator (its run in standalone mode so it can work as a worker as well so
workers aren't needed), you can `kubectl exec` into the presto container, and run the presto client.

Then, list the catalogs:

```
presto> show CATALOGS;
 Catalog 
---------
 minio
 mysql
 system
 tpch
(4 rows)

Query 20190831_191859_00000_c7y4q, FINISHED, 1 node
Splits: 19 total, 19 done (100.00%)
0:02 [0 rows, 0B] [0 rows/s, 0B/s]
```

The minio catalog above is a queriable SQL data store which you can run regular presto queries against.

The hive metastore is started by these images, which were originally created by engineers at Minio.

For future work, we should create our own Minio images which can do this in a modular way, and
put more configmap options into the containers themselves, possibly injecting many or all presto config via
a config map YAML (as is done for spark/kafka/in this repo).