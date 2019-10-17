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

...is a project for the development of packaging and tests of the Big Data and Data Analytics ecosystem.

The primary goal of Apache Bigtop is to build a community around the packaging and interoperability testing of bigdata-related projects. This includes testing at various levels (packaging, platform, runtime, upgrade, etc...) developed by a community with a focus on the system as a whole, rather than individual projects.

The simplest way to get a feel for how bigtop works, is to just cd into `provisioner` and try out the recipes under vagrant or docker.  Each one rapidly spins up, and runs the bigtop smoke tests on, a local bigtop based big data distribution. Once you get the gist, you can hack around with the recipes to learn how the puppet/rpm/smoke-tests all work together, going deeper into the components you are interested in as described below.

# Quick overview of source code directories

* __bigtop-deploy__ : deployment scripts and puppet stuff for Apache Bigtop.
* __bigtop-packages__ : RPM/DEB specifications for Apache Bigtop subcomponents.
* __bigtop-test-framework__ : The source code for the iTest utilities (framework used by smoke tests).
* __bigtop-tests__ :
* __test-artifacts__ : source for tests.
* __test-execution__ : maven pom drivers for running the integration tests found in test-artifacts.
* __bigtop-toolchain__ : puppet scripts for setting up an instance which can build Apache Bigtop, sets up utils like jdk/maven/protobufs/...
* __provisioner__ : Vagrant and Docker Provisioner that automatically spin up Hadoop environment with one click.
* __docker__ : Dockerfiles and Docker Sandbox build scripts.

Also, there is a new project underway, Apache Bigtop blueprints, which aims to create templates/examples that demonstrate/compare various Apache Hadoop ecosystem components with one another.

# Contributing

There are lots of ways to contribute.  People with different expertise can help with various subprojects:

* __puppet__ : Much of the Apache Bigtop deploy and packaging tools use puppet to bootstrap and set up a cluster. But recipes for other tools are also welcome (ie. Chef, Ansible, etc.)
* __groovy__ : Primary language used to write the Apache Bigtop smokes and itest framework.
* __maven__ : Used to build Apache Bigtop smokes and also to define the high level Apache Bigtop project.
* __contributing your workloads__ : Contributing your workloads enable us to tests projects against real use cases and enable you to have people verifying the use cases you care about are always working.
* __documentation__ : We are always in need of a better documentation!
* __giving feedback__ : Tell us how you use Apache Bigtop, what was great and what was not so great. Also, what are you expecting from it and what would you like to see in the future?

Also, opening [JIRA's](https://issues.apache.org/jira/browse/BIGTOP) and getting started by posting on the mailing list is helpful.

# Cloud Native Bigtop

This is the content for the talk given by jay vyas and sid mani @ apachecon 2019 in Las Vegas,  you can watch it here  https://www.youtube.com/watch?v=LUCE63q !

## TLDR, heres how you create an analytics distro on K8s...

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
## Problem

Installation of things has been commoditized by containers and K8s.  The more important
problems we have nowadays are around interoperation, learning, and integration of different
tools for different problems in the analytics space.

Modern data scientists need 'batteries included' frameworks that can be used to model and
address different types of analytics problems over time, which can replicate the integrated
functionality of AWS, GCP, and so on.

## Current Status

This repository currently integrates installation of a full analytics stack for kubernetes
with batteries included, including storage.

## Modifications from generic charts or recipes

configuration isnt really externalized very well in most off the shelf helm charts.  The other obvious missing link is that storage isnt provided for you, which is a problem for folks that don't know how to do things in K8s.   We've externalized configuration for all files (i.e. see spark as a canonical example of this) into configmaps and unified zookeeper instances into a single instances for ease of deployment here.  Also, this repo has *tested* different helm repos / yaml files to see what random code on the internet actually works
the way it should.  

For example, the stable helm charts don't properly configure zepplin, allow for empty storage on ZK, or inject config into kafka as you'd want to be able to in certain scenarios.  In this repo, everything should *just work* provided you create things in *the right order*.


# Immediately Get Started with Deployment and Smoke Testing of Cloud Native BigTop

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

```
```
k8s-1$ kubectl plugin list
k8s-1$ kubectl bigtop kubectl-config && kubectl bigtop helm-install

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

# Getting Started

Below are some recipes for getting started with using Apache Bigtop. As Apache Bigtop has different subprojects, these recipes will continue to evolve.
For specific questions it's always a good idea to ping the mailing list at dev-subscribe@bigtop.apache.org to get some immediate feedback, or [open a JIRA](https://issues.apache.org/jira/browse/BIGTOP).

For Users: Running the smoke tests
-----------------------------------

The simplest way to test bigtop is described in bigtop-tests/smoke-tests/README file

For integration (API level) testing with maven, read on.

For Developers: Building and modifying the web site
---------------------------------------------------

The website can be built by running `mvn site:site` from the root directory of the
project.  The main page can be accessed from "project_root/target/site/index.html".

The source for the website is located in "project_root/src/site/".


# Contact us

You can get in touch with us on [the Apache Bigtop mailing lists](http://bigtop.apache.org/mail-lists.html).
