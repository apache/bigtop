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

----------------------------------------------------------------------------

# BigTop OpenStack VM Provisioner

## Overview 

This vagrant recipe is based on the vagrant recipe from `vagrant-puppet-vm` with added feature of vagrant-openstack-provider plugin. The plugin allows us to deploy a Hadoop cluster on an actual virtual environment as if we are deploying on local vagrant vms. It will spin up and provision the vm(s) for us.

The Vagrantfile creates a BigTop virtual Hadoop cluster on OpenStack by using BigTop puppet recipes and pulling from existing bigtop repositories

When the configuration is correctly set up in vagrantconfig.yaml, we should be able to deploy a cluster with on single command `vagrant up`

This can be use:

* to deploy BigTop Hadoop cluster(s) on an OpenStack cloud environment
* to run BigTop smoke tests on the cluster

## Usage

0) Set up environment 

Install vagrant from [official website](www.vagrantup.com) 

If you want to provision machines in parallel, install gnu-parallel 

```
# for centos
yum install parallel 
# for mac
brew install parallel
# for debian/ubuntu
apt-get install parallel
```

1) Install [vagrant-hostmanager plugin](https://github.com/smdahlen/vagrant-hostmanager) to better manage `/etc/hosts`

```
vagrant plugin install vagrant-hostmanager
```

2) Install [vagrant-openstack-provider](https://github.com/ggiamarchi/vagrant-openstack-provider) 

```
vagrant plugin install vagrant-openstack-provider
```

3) Set up configuration 

For now this is partically handled by openstack rc file and partically handled by vagrantconfig.yaml file

Download rc file from Openstack Horizon dashbord Access & Security and run
```
source projectname-openrc.sh
```
You will also need to specify flavor, image_id, keypair_name, and FQDN (fully qualified domain name of your openstack environment) in vagrantconfig.yaml to successfully spin up a vm

```
flavor: "name of your choice of flavor" # e.g. m1.small 
image_id: "UUID of your choice of image" # e.g. 8fddf8aa-1809-414d-b478-f93b8415f5f4
keypair_name: "your key pair name on openstack" # e.g. cloud-key
FQDN: "the fully qualified domain name of the environment"  
key_path: "location of your private key" #e.g. ~/.ssh/cloud-key.pem 
```

There are other options in vagrantconfig.yaml that you can specify such as set number of vms in the cluster, and automatically run smoke tests

The `run_in_parallel` options should be set to true if want to provision machins in parallel. see below for how to. 

```
num_instance: 1
run_smoke_tests: true
run_in_parallel: false
```

You can also determine what components are being installed and tested

```
components: [hadoop, yarn]
smoke_test_components: [mapredcue, pig]
```

## GO

For deployment in sequence 

1. set `run_in_parallel` option in `vagrantconfig.yaml` to false
2. run
```
vagrant up --provider=openstack
```

For parallel provisioning:

1. set `run_in_parallel` option in `vagrantconfig.yaml` to true
2. run
```
./para-provision.sh
```

#### Parallel provisioning

**This script is based on Joe Miller's para-vagrant.sh script please see NOTICE for more information**

Script reads parameter `num_instance`, `run_smoke_tests` and `smoke_test_components` from `vagrantconfig.yaml` to determine how many machines to spin up and whether or not to run smoke tests and what components will be tested

This script will spin up vms on openstack sequentially first, and then do the provisioning in parallel. Each guest machine will have it's own log file. And will generate a log file for smoke tests if `run_smoke_tests` set to true 

There are some sketchy places in the code...(cuz I suck in bash) such as: 
* still have unprintable ^M in log file. I haven't figure out how to get rid of them without destroying the format yet
* use of `sed`: so OS X hates me, and it won't let me use `sed -r` so again I create another temporary file for the smoke tests log

**NOTE**: 
* the name of the vm in `Vagrantfile` **has to** match the name of the vim in `para-provision.sh`, you can change them in line 66

```
for ((i=1; i<=$NUM_INSTANCE; i++)); do
    cat <<EOF
name_of_your_vm$i
EOF
```
* If you are also running smoke tests, you will have to change the machine name in line 74 

```
    vagrant ssh name_of_your_vm$NUM_INSTANCE -c "sudo su <<HERE 
```

* In `Vagrantfile` the run smoke test part is currently commented out, in order to run smoke test after **all** the machines are done provisioning. The smoke tests are currently being handled by `para-provision.sh`. 

#### TODO

* test installing all available components
  * spark2 is not working, looks like a puppet problem
* test all the provided smoke tests
  * mahout smoke tests didn't run all the way throught
* enable_local_repo
* modify the code to make it more generic, I only tried this on Centos 6
