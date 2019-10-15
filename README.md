[![Travis CI](https://img.shields.io/travis/apache/bigtop.svg?branch=master)](https://travis-ci.org/apache/bigtop)

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

TBD

# Get Started with Deployment and Smoke Testing of Cloud Native BigTop

Prerequisites:
- Vagrant
- Java

## Set up 3-Node Kubernetes cluster via Kubespray on local machine
```
$ cd $BIGTOP_HOME
$ ./gradlew kubespray-clean kubespray-download && cd dl/ && tar xvfz kubespray-2.11.0.tar.gz
$ cd dl/kubespray-2.11.0/ && cp ../../kubespray/vagrant/Vagrantfile .
$ vagrant up
```

## Configuring ```kubectl``` for local cluster
```
$ vagrant ssh k8s-1

k8s-1$ mkdir -p ~/.kube
k8s-1$ sudo cp /etc/kubernetes/admin.conf .kube/config
k8s-1$ sudo chown -R vagrant:vagrant .kube
k8s-1$ kubectl cluster-info

Kubernetes master is running at https://172.17.8.101:6443
coredns is running at https://172.17.8.101:6443/api/v1/namespaces/kube-system/services/coredns:dns/proxy
kubernetes-dashboard is running at https://172.17.8.101:6443/api/v1/namespaces/kube-system/services/https:kubernetes-dashboard:/proxy

To further debug and diagnose cluster problems, use 'kubectl cluster-info dump'.
```

## Storage
You need to install ```lvm2``` package for Rook-Ceph:
```
# Centos
sudo yum install -y lvm2

# Ubuntu
sudo apt-get install -y lvm2
```
Refer to https://rook.io/docs/rook/v1.1/k8s-pre-reqs.html for prerequisites on Rook

Run ```download``` task to get Rook binary:
```
$ ./gradlew rook-clean rook-download && cd dl/ && tar xvfz rook-1.1.2.tar.gz
```

Create Rook operator:
```
$ kubectl create -f dl/rook-1.1.2/cluster/examples/kubernetes/ceph/common.yaml
$ kubectl create -f dl/rook-1.1.2/cluster/examples/kubernetes/ceph/operator.yaml
$ kubectl -n rook-ceph get pod
```

Create Ceph cluster:
```
# test
$ kubectl create -f storage/rook/ceph/cluster-test.yaml

# production
$ kubectl create -f storage/rook/ceph/cluster.yaml

$ kubectl get pod -n rook-ceph
```

Deploy Ceph Toolbox:
```
$ kubectl create -f dl/rook-1.1.2/cluster/examples/kubernetes/ceph/toolbox.yaml
$ kubectl -n rook-ceph get pod -l "app=rook-ceph-tools"
$ kubectl -n rook-ceph exec -it $(kubectl -n rook-ceph get pod -l "app=rook-ceph-tools" -o jsonpath='{.items[0].metadata.name}') bash

# ceph status &&
ceph osd status &&
ceph df &&
rados df &&
```
Refer to https://rook.io/docs/rook/v1.1/ceph-toolbox.html for more details.

Create a StorageClass for Ceph RBD:
```
$ kubectl create -f dl/rook-1.1.2/cluster/examples/kubernetes/ceph/csi/rbd/storageclass.yaml
kubectl get storageclass
rook-ceph-block
```

Create Minio operator:
```
$ kubectl create -f dl/rook-1.1.2/cluster/examples/kubernetes/minio/operator.yaml

#
$ kubectl -n rook-minio-system get pod
```
```
$ kubectl create -f storage/rook/minio/object-store.yaml
$ kubectl -n rook-minio get pod -l app=minio,objectstore=my-store
```

## Zookeeper
Deploy Zookeeper cluster on Kubernetes cluster via Helm chart:
```
$ cd $BIGTOP_HOME
$ export NS="bigtop"
$ helm repo add incubator http://storage.googleapis.com/kubernetes-charts-incubator
$ helm install --name zookeeper --namespace $NS -f zookeeper/values.yaml incubator/zookeeper
$ kubectl get all -n $NS -l app=zookeeper
$ kubectl exec -n $NS zookeeper-0 -- bin/zkServer.sh status
```
Refer to https://github.com/helm/charts/tree/master/incubator/zookeeper for more configurations.

## Kafka
Deploy Kafka cluser via Helm:
```
$ helm repo add incubator http://storage.googleapis.com/kubernetes-charts-incubator
$ helm install --name kafka \
--namespace bigtop \
-f kafka/values.yaml \
--set zookeeper.url="zookeeper-0.zookeeper-headless:2181\,zookeeper-1.zookeeper-headless:2181\,zookeeper-2.zookeeper-headless:2181" \
incubator/kafka

# Deploy Kafka client:
$ kubectl create --namespace bigtop -f kafka/kafka-client.yaml

# Usage of Kafka client
$ export ZOOKEEPER_URL="zookeeper-0.zookeeper-headless:2181,zookeeper-1.zookeeper-headless:2181,zookeeper-2.zookeeper-headless:2181"

# List all topics
$ kubectl -n bigtop exec kafka-client -- kafka-topics \
--zookeeper  $ZOOKEEPER_URL \
--list

# To create a new topic:
$ kubectl -n bigtop exec kafka-client -- kafka-topics \
--zookeeper $ZOOKEEPER_URL \
--topic test1 --create --partitions 1 --replication-factor 1

```

### Schema Registry 
Optionally, You can create schema registry service for Kafka:
```
helm install --name kafka-schema-registry --namespace bigtop -f kafka/schema-registry/values.yaml \
--set kafkaStore.overrideBootstrapServers="kafka-0.kafka-headless:9092\,kafka-1.kafka-headless:9092\,kafka-2.kafka-headless:9092" \
incubator/schema-registry

```

Getting Started
===============

Below are some recipes for getting started with using Apache Bigtop. As Apache Bigtop has different subprojects, these recipes will continue to evolve.
For specific questions it's always a good idea to ping the mailing list at dev-subscribe@bigtop.apache.org to get some immediate feedback, or [open a JIRA](https://issues.apache.org/jira/browse/BIGTOP).

For Users: Running the smoke tests
-----------------------------------

The simplest way to test bigtop is described in bigtop-tests/smoke-tests/README file

For integration (API level) testing with maven, read on.

# Cloud Native Bigtop
This is the content for the talk given by jay vyas and sid mani @ apachecon 2019 in Las Vegas,  you can watch it here  https://www.youtube.com/watch?v=LUCE63q !

# TLDR, heres how you create an analytics distro on K8s...

```
helm install stable/nfs-server-provisioner ; kubectl patch storageclass nfs -p '{"metadata": {"annotations":{"storageclass.kubernetes.io/is-default-class":"true"}}}'
Minio:  kubectl -n minio create secret generic my-minio-secret --from-literal=accesskey=minio --from-literal=secretkey=minio123
helm install --set existingSecret=my-minio-secret stable/minio --namespace=minio --name=minio
Nifi: helm repo add cetic https://cetic.github.io/helm-charts ; helm install nifi --namespace=minio
Kafka:  helm repo add incubator http://storage.googleapis.com/kubernetes-charts-incubator $ helm install --name my-kafka incubator/kafka , kubectl edit statefulset kafka
 envFrom:
        - configMapRef:
            name: kafka-cm
Spark: kubectl create configmap spark-conf --from-file=core-site.xml --from-file=log4j.properties --from-file=spark-defaults.conf --from-file=spark-env.sh -n bigdata ; helm install microsoft/spark --version 1.0.0 --namespace=minio
Presto: cd ./presto3-minio/ , kubectl create -f - -n minio

```

# Problem

Installation of things has been commoditized by containers and K8s.  The more important
problems we have nowadays are around interoperation, learning, and integration of different
tools for different problems in the analytics space.

Modern data scientists need 'batteries included' frameworks that can be used to model and
address different types of analytics problems over time, which can replicate the integrated
functionality of AWS, GCP, and so on.

# Current Status

This repository currently integrates installation of a full analytics stack for kubernetes
with batteries included, including storage.

```
                       +----------------+
                       |                |    XXX           XXX          XXXXXX
                       |    NIFI        |XXXXX  XXX       XX  XXX     XXX    XX
                       |                |         XX    XXX     XX    X       XX
                       |                |          XXXXXX        XXXXXX        X
                       +-----+----------+                                     X
+-------------+              |                                                X
|             |              |                                                XXXXXX
|    Kafka    |              |                                                      XXXX
|             |              |                         +----------------+           XXXX
+-----+-------+              |                         |                |     XXXXXXX
      |                      |                         |  Zepplin       |    XX
      |               +------v------+                  |                |    XXXXXX
      +-------------->+             |                  |                |         X
                      |    Zookeeper+-------+          +-----------+----+         X
                      |             |       |                      |           X  X  XX
                      +-------------+       |                      |           XX X XX
                                            |                      |            XXXXX
                                            |                      |
                                            |                      |  +--------v------+
                                            v                      +> | Spark         |
                                    +-------+----------+---+          |               |
                                    |                  |   |          |               |
                                    |    Volume PRovisioner|          +---------------+
                                    |    (NFS or hostpath) |
                                    |                  |   |
                                    +-------------^----+---+ .            (Presto)
                                                  ^                          |
                                                  |                          |
                                                  |                          V
                                                  |                +---------------+
                                                  |                |               |
                                                  |                |               |
                                                  +----------------+   Minio       |
                                                                   |               |
                                                                   +---------------+
```

If all services are deployed succesfully, you ultimately will have an inventory looking like this:


```
$> kubectl get pods -n bigdata
NAME                                          READY   STATUS    RESTARTS   AGE
coordinator-56956c8d84-hgxvc                  1/1     Running   0          34s
fantastic-chipmunk-livy-5856779cf8-w8wlr      1/1     Running   0          3d1h
fantastic-chipmunk-master-55f5945997-mbvbm    1/1     Running   0          3d
fantastic-chipmunk-worker-5f7f468b8f-mwnmg    1/1     Running   1          3d1h
fantastic-chipmunk-worker-5f7f468b8f-zkbrw    1/1     Running   0          3d1h
fantastic-chipmunk-zeppelin-7958b9477-vv25d   1/1     Running   0          3d1h
hbase-hbase-master-0                          1/1     Running   0          4h4m
hbase-hbase-rs-0                              1/1     Running   2          4h7m
hbase-hbase-rs-1                              1/1     Running   1          4h5m
hbase-hbase-rs-2                              1/1     Running   0          4h4m
hbase-hdfs-dn-0                               1/1     Running   1          4h7m
hbase-hdfs-dn-1                               1/1     Running   0          4h5m
hbase-hdfs-dn-2                               1/1     Running   0          4h5m
hbase-hdfs-nn-0                               1/1     Running   0          4h7m
minio-7bf4678799-cd8qz                        1/1     Running   0          3d22h
my-kafka-0                                    1/1     Running   0          27h
my-kafka-1                                    1/1     Running   0          27h
my-kafka-2                                    1/1     Running   0          27h
nifi-0                                        4/4     Running   0          2d3h
nifi-zookeeper-0                              1/1     Running   0          2d3h
nifi-zookeeper-1                              1/1     Running   0          2d3h
nifi-zookeeper-2                              1/1     Running   0          2d3h
worker-565c7c858-pjlpg                        1/1     Running   0          34s
```

# Modifications from generic charts or recipes

configuration isnt really externalized very well in most off the shelf helm charts.  The other obvious missing link is that storage isnt provided for you, which is a problem for folks that don't know how to do things in K8s.   We've externalized configuration for all files (i.e. see spark as a canonical example of this) into configmaps and unified zookeeper instances into a single instances for ease of deployment here.  Also, this repo has *tested* different helm repos / yaml files to see what random code on the internet actually works
the way it should.  

For example, the stable helm charts don't properly configure zepplin, allow for empty storage on ZK, or inject config into kafka as you'd want to be able to in certain scenarios.  In this repo, everything should *just work* provided you create things in *the right order*.

# Instructions.

1. First , install an NFS volume provisioner from the instructions storage/ directory
2. Then follow the other instructions in the storage README
3. Now, install components one by one from the README.md files in the processing/ directory.

This will yield the following analytics distro, all running in the bigdata namespace (make sure to use
`--namespace=bigdata` or similar on all `helm install` or `kubectl create` directives).  IF you mess anything up
do `helm list` (find your installation, i.e. XYZ) followed by `helm delete XYZ`  to clear out your components.

In particular, this repo modifies stock helm charts in a variety of ways to make things work together.

1. We don't use stable/spark because its *old*.  Instead we use microsofts spark, which comes integrated
with zepplin properly.
2. We use configmaps for configuration of *spark*.  For spark, this allows us to inject
different types of configuration stuff from the kuberentes level, rather then baking them into the image (note that
you cant just inject a single file from a config map, b/c it overwrites the whole directory).  This allows us
to inject minio access properties into spark itself, while also injecting other config.
3. For Kafka, we config map the environment variables so that we can use the same zookeeper instance as
NiFi.  
4. For Presto, the configuration parameters for workers/masters are all injected also via config map.  We use
a fork of https://github.com/dharmeshkakadia/presto-kubernetes for this change (PR's are submitted to make this upstream).
5. For minio there arent any major changes needed out of the box, except using emptyDir for storage if you dont have a volume provisioner.
6. For HBase, we also reuse the same zookeeper instance that is used via NIFI and kafka.  For now we use the nifi zk deployment but at some point we will make ZK a first class citizen.

============================================

Notes and Ideas

# Inspiration

Recently saw https://github.com/dacort/damons-data-lake.
- A problem set that is increasingly relevant: lots of sources, real time, unstructured warehouse/lake.
- No upstream plug-and-play alternative to cloud native services stack.
- Infrastructure, storage, networking is the hardest part.
