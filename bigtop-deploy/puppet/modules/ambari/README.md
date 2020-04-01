# Deploy Bigtop with Ambari Management Pack
[![Ambari](https://ambari.apache.org/images/apache-ambari-project.png)](https://https://ambari.apache.org/)

### Background
- Apache Ambari is an open-source central management platform for provisioning, managing, monitoring and securing Hadoop clusters.
- Ambari Management packs allow you to deploy a range of services to the Hadoop-cluster. You can use a management pack to deploy a specific component or service, or to deploy an entire platform, like HDFS.
- We define a Ambari Management Pack, BGTP, that targets to address the needs of deploying and running Hadoop-related services by Bigtop facilities.


### Build and Installation
Build `DEB`/`RPM` from Bigtop docker provisioner:

```sh
$ docker run --rm -v `pwd`:/ws --workdir /ws bigtop/slaves:trunk-centos-7 bash -l -c './gradlew allclean ; ./gradlew ambari-pkg'
```

### Ambari Nodes Preparation and Configuration
Install Ambari-server and Ambari-agent packages in the nodes respectively: 
```sh
Node1: ambari-server 
Node2: ambari-agent01
Node3: ambari-agent02
```

Modify Ambari server hostname to `/etc/ambari-agent/conf/ambari-agent.ini` which is located on Ambari agent nodes:
```sh
[server]
hostname='Ambari-server-hostname'
url_port=8440
secured_url_port=8441
...
..
[security]:
force_https_protocol=PROTOCOL_TLSv1_2
...
..
```
Disable `HTTPS certificate verification`
`/etc/python/cert-verification.cfg`:
```sh
[https]
verify=disable
```

Install `chrony` and start chronyd service for time-sync among nodes.
```sh
$ yum install chrony
$ systemctl start chronyd
$ systemctl enable chronyd
```

### BGTP Mpack Installation
Install bgtp-mapck on Ambari-server:
```sh
sudo ambari-server install-mpack --mpack=/path/bgtp-ambari-mpack-1.0.0.0-SNAPSHOT-bgtp-ambari-mpack.tar.gz --purge --verbose
```
`/path` is located in Ambari-server: `/var/lib/ambari-server/resources`
You can modify the location of `/path` in [ install_ambari.sh](https://github.com/apache/bigtop/blob/master/bigtop-packages/src/common/ambari/install_ambari.sh).

### Start Ambari Server and Agents
Start Ambari-server node:
```sh
ambari-server reset
ambari-server setup
ambari-server start
```
Start Ambari-agent nodes:
```sh
ambari-agent restart
```

### Deploy Hadoop by Ambari facility
Access Ambari WebUI to take manual depolyment with BGTP Mpack:
```sh
ambari-server-ip:8080 
admin/admin
```
Deployment with Ambari-WebUI demo:  [here](https://www.youtube.com/watch?v=vnyUQtF8ZyM).

