This is a proof of concept provisioner based on docker local clustering

NAME RESOLUTION
===============
In order to enable docker DNS we need a docker network
Create with (need only to be done once)
# docker network create --driver=bridge bigtop.org

Setting the hostname to the service name enables us to have FQDN
of the hosts set correctly in /etc/hosts

x.y.z.t headnode.bigtop.org headnode

This is important since name resolution in /etc/nsswitch.conf is 
hosts: files, dns

The other members of the cluster datanode1 and datanode2 will be resolved by DNS(and headnode by DNS for these cluster nodes respectivly)

External network enables us to have control over the domain name of the
cluster DNS

VOLUMES
========
In order to have performant I/O we use volumes


CREATING CLUSTER
================
# docker-compose up -d

This builds (if not already done) the initial container, containing all puppet configs.

PROVISIONING
============

# docker-compose exec headnode  puppet apply --parser=future /etc/puppet/manifests

run puppet on headnode, install bigtop and config. dito for other nodes. Can be done in parallel

# docker-compose exec datanode1 puppet apply --parser=future /etc/puppet/manifests
# docker-compose exec datanode2 puppet apply --parser=future /etc/puppet/manifests

Cluster is now up and running

UPDATING CONFIG
===============

If you are changing puppet scripts, run

# docker-compose build

Not sure if we are loosing HDFS data this way.


TEARING DOWN
============

docker-compose down -v
# since we are allocating volumes, remove them too

