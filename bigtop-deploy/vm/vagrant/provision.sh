#Taken from : https://cwiki.apache.org/confluence/display/BIGTOP/How+to+install+Hadoop+distribution+from+Bigtop+0.6.0
#A Vagrant recipe for setting up a hadoop box.

#Get the apache yum repo
sudo yum install -y wget java-1.7.0-openjdk-devel.x86_64

wget -O /etc/yum.repos.d/bigtop.repo http://www.apache.org/dist/bigtop/bigtop-0.6.0/repos/fedora18/bigtop.repo

#Now install the base components
sudo yum install -y  hadoop\* mahout\* hive\* pig\*

export JAVA_HOME=`sudo find /usr -name java-* | grep openjdk | grep 64 | grep "jvm/java" | grep -v fc`

sudo /etc/init.d/hadoop-hdfs-namenode init

#Start each datanode
for i in hadoop-hdfs-namenode hadoop-hdfs-datanode ; 
	do sudo service $i start ; 
done

sudo /usr/lib/hadoop/libexec/init-hdfs.sh

sudo service hadoop-yarn-resourcemanager start
sudo service hadoop-yarn-nodemanager start

hadoop fs -ls -R /

# Make a directory so that vagrant user has a dir to run jobs inside of. 
hadoop fs -mkdir /user/vagrant && hadoop fs -chown vagrant:vagrant /user/vagrant

hadoop jar /usr/lib/hadoop-mapreduce/hadoop-mapreduce-examples*.jar pi 2 2