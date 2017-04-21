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

Juju Charms allow you to deploy, configure, and connect an Apache Bigtop cluster
on any supported cloud, which can be scaled to meet workload demands. You can
also easily connect other, non-Bigtop components from the [Juju charm store][]
that support common interfaces.

This source tree contains the charm layers used to build charms for deploying
Bigtop components.  Built charms are published to the [Juju charm store][]
and can be deployed directly from there, either individually or with
[bundles][]. They can also be built from these layers and deployed locally.

For the remainder of this guide, a working Juju 2.0 or greater installation is
assumed to be present. If Juju is not yet set up, please follow the
[getting-started][] instructions prior to deploying locally built charms and
bundles.

[Juju charm store]: https://jujucharms.com/
[bundles]: https://jujucharms.com/hadoop-processing
[getting-started]: https://jujucharms.com/docs/stable/getting-started


## Building the Bigtop Charms

To build these charms, you will need [charm-tools][]. You should also read
over the developer [Getting Started][] page for an overview of developing and
building charms. Then, in any of the charm layer directories, use `charm build`.
For example:

    export JUJU_REPOSITORY=$HOME/charms
    mkdir $JUJU_REPOSITORY

    cd bigtop-packages/src/charms/hadoop/layer-hadoop-namenode
    charm build --series xenial --report

This will build the NameNode charm in the
$JUJU_REPOSITORY/xenial/hadoop-namenode directory, pulling in the appropriate
base and interface layers from [interfaces.juju.solutions][].  You can get
local copies of those layers as well by using `charm pull-source`:

    export LAYER_PATH=$HOME/layers
    export INTERFACE_PATH=$HOME/interfaces
    mkdir $HOME/{layers,interfaces}

    charm pull-source layer:apache-bigtop-base
    charm pull-source interface:dfs

You can deploy the locally built charms individually, for example:

    juju deploy $JUJU_REPOSITORY/xenial/hadoop-namenode

You can also deploy the local version of a bundle:

    juju deploy ./bigtop-deploy/juju/hadoop-processing/bundle-local.yaml

[charm-tools]: https://jujucharms.com/docs/stable/tools-charm-tools
[Getting Started]: https://jujucharms.com/docs/stable/developer-getting-started
[interfaces.juju.solutions]: http://interfaces.juju.solutions/
[juju-quickstart]: https://launchpad.net/juju-quickstart
