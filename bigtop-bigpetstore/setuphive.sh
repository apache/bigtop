### THIS SCRIPT SETS UP HIVE AND HADOOP TARBALLS FOR YOU ###
HIVE_TARBALL="http://archive.apache.org/dist/hive/hive-0.12.0/hive-0.12.0.tar.gz"
HADOOP_TARBALL="https://archive.apache.org/dist/hadoop/core/hadoop-1.2.1/hadoop-1.2.1.tar.gz"
wget $HIVE_TARBALL
wget $HADOOP_TARBALL


# REMEBER SO WE CAN CD BACK AT END 
mydir=`pwd`

## HADOOP SETUP
mkdir -p /opt/bigpetstore
cd /opt/bigpetstore
tar -xvf hadoop-1.2.1.tar.gz
export HADOOP_HOME=`pwd`/hadoop-1.2.1

## HIVE SETUP 
tar -xvf hive-0.12.0.tar.gz
cp /opt/hive-0.12.0/lib/hive*.jar $HADOOP_HOME/lib

## CD BACK TO ORIGINAL DIR
cd $mydir
