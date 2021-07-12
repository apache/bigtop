# Dockerfile to deploy a bigtop build
FROM centos
MAINTAINER BigTop, http://bigtop.apache.org

# optional
ENV http_proxy http://10.119.25.137:3128

RUN yum install -y wget java-1.7.0-openjdk-devel.x86_64
RUN wget -O /etc/yum.repos.d/bigtop.repo http://www.apache.org/dist/bigtop/stable/repos/centos6/bigtop.repo
RUN yum install -y  hadoop\* mahout\* hive\* pig\*

ENV JAVA_HOME /usr/lib/jvm/default

RUN /etc/init.d/hadoop-hdfs-namenode init
RUN service hadoop-hdfs-namenode start
RUN service hadoop-hdfs-datanode start
RUN /usr/lib/hadoop/libexec/init-hdfs.sh
RUN service hadoop-yarn-resourcemanager start
RUN service hadoop-yarn-nodemanager start
RUN hadoop fs -ls -R /
RUN echo $USER
