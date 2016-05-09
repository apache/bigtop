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
# Juju Charms for Deploying Bigtop

## Overview

These are the charm layers used to build Juju charms for deploying Bigtop
components.  The charms are also published to the [Juju charm store][] and
can be deployed directly from there using [bundles][], or they can be
built from these layers and deployed locally.

Charms allow you to deploy, configure, and connect a Apache Bigtop cluster
on any supported cloud, which can be easily scaled to meet workload demands.
You can also easily connect other, non-Bigtop components from the
[Juju charm store][] that support common interfaces.


[Juju charm store]: https://jujucharms.com/
[bundles]: https://jujucharms.com/u/bigdata-dev/hadoop-processing


## Building the Bigtop Charms

To build these charms, you will need [charm-tools][].  You should also read
over the developer [Getting Started][] page for an overview of charms and
building them.  Then, in any of the charm layer directories, use `charm build`.
For example:

    export JUJU_REPOSITORY=$HOME/charms
    mkdir $HOME/charms

    cd bigtop-packages/src/charms/hadoop/layer-hadoop-namenode
    charm build

This will build the NameNode charm, pulling in the appropriate base and
interface layers from [interfaces.juju.solutions][].  You can get local copies
of those layers as well using `charm pull-source`:

    export LAYER_PATH=$HOME/layers
    export INTERFACE_PATH=$HOME/interfaces
    mkdir $HOME/{layers,interfaces}

    charm pull-source layer:apache-bigtop-base
    charm pull-source interface:dfs

You can then deploy the locally built charms individually:

    juju deploy local:trusty/hadoop-namenode

You can also use the local version of a bundle:

    juju deploy bigtop-deploy/juju/hadoop-processing/bundle-local.yaml

> Note: With Juju versions < 2.0, you will need to use [juju-deployer][] to
deploy the local bundle.


[charm-tools]: https://jujucharms.com/docs/stable/tools-charm-tools
[Getting Started]: https://jujucharms.com/docs/devel/developer-getting-started
[interfaces.juju.solutions]: http://interfaces.juju.solutions/
[juju-deployer]: https://pypi.python.org/pypi/juju-deployer/
