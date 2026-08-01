#!/bin/sh

# concurrency is hard, let's have a beer

# This script is based onthe `para-vagrant.sh` script by Joe Miller, available at https://github.com/joemiller/sensu-tests/blob/master/para-vagrant.sh.
# see NOTICE file

# any valid parallel argument will work here, such as -P x.
MAX_PROCS="-j 10"

# Read parameter from vagrantconfig.yaml file
NUM_INSTANCE=$(grep num_instance vagrantconfig.yaml | awk -F: '/:/{gsub(/ /, "", $2); print $2}')
SMOKE_TEST_COMPONENTS=$(grep smoke_test_components vagrantconfig.yaml | awk -F[ '/,/{gsub(/ /, "", $2); print $2}' | awk -F] '{print $1}')
RUN_SMOKE_TESTS=$(grep run_smoke_tests vagrantconfig.yaml | awk -F: '/:/{gsub(/ /, "", $2); print $2}')

parallel_provision() {
    while read box; do
        echo $box
     done | parallel $MAX_PROCS -I"NODE" -q \
        sh -c 'LOGFILE="logs/NODE.out.txt" ;                                 \
                printf  "[NODE] Provisioning. Log: $LOGFILE, Result: " ;     \
                vagrant provision NODE > $LOGFILE 2>&1 ;                      \
                echo "vagrant provision NODE > $LOGFILE 2>&1" ;               \
                RETVAL=$? ;                                                 \
                if [ $RETVAL -gt 0 ]; then                                  \
                    echo " FAILURE";                                        \
                    tail -12 $LOGFILE | sed -e "s/^/[NODE]  /g";             \
                    echo "[NODE] ---------------------------------------------------------------------------";   \
                    echo "FAILURE ec=$RETVAL" >>$LOGFILE;                   \
                else                                                        \
                    echo " SUCCESS";                                        \
                    tail -5 $LOGFILE | sed -e "s/^/[NODE]  /g";              \
                    echo "[NODE] ---------------------------------------------------------------------------";   \
                    echo "SUCCESS" >>$LOGFILE;                              \
                fi;                                                         \
                exit $RETVAL'

    failures=$(egrep  '^FAILURE' logs/*.out.txt | sed -e 's/^logs\///' -e 's/\.out\.txt:.*//' -e 's/^/  /')
    successes=$(egrep '^SUCCESS' logs/*.out.txt | sed -e 's/^logs\///' -e 's/\.out\.txt:.*//' -e 's/^/  /')

    echo
    echo "Failures:"
    echo '------------------'
    echo "$failures"
    echo
    echo "Successes:"
    echo '------------------'
    echo "$successes"
}

## -- main -- ##

# cleanup old logs
mkdir logs >/dev/null 2>&1
rm -f logs/*

# spin up vms sequentially, because openstack provider doesn't support --parallel 
# This step will update `/etc/hosts` file in vms, because since version 1.5 vagrant up runs hostmanager before provision 
echo ' ==> Calling "vagrant up" to boot the vms...'
vagrant up --no-provision

# but run provision tasks in parallel
echo " ==> Beginning parallel 'vagrant provision' processes ..."
for ((i=1; i<=$NUM_INSTANCE; i++));do
    cat <<EOF
hadoop-bigtop$i 
EOF
done | parallel_provision   

#run smoketest on the last node when all node finish provisioning 
echo "preparing for smoke tests..."
if [ "$RUN_SMOKE_TESTS" = "true" ]; then
    echo "running smoke tests..."
    vagrant ssh hadoop-bigtop$NUM_INSTANCE -c "sudo su <<HERE
    cd /bigtop-home/bigtop-tests/smoke-tests 
    export HADOOP_CONF_DIR=/etc/hadoop/conf/ 
    export BIGTOP_HOME=/bigtop-home/ 
    export HADOOP_MAPRED_HOME=/usr/lib/hadoop-mapreduce/ 
    export HIVE_HOME=/usr/lib/hive/ 
    export PIG_HOME=/usr/lib/pig/ 
    export FLUME_HOME=/usr/lib/flume/ 
    export HIVE_CONF_DIR=/etc/hive/conf/ 
    export JAVA_HOME=/usr/lib/jvm/java-openjdk/ 
    export MAHOUT_HOME=/usr/lib/mahout 
    export ITEST="0.7.0" 

    su -s /bin/bash $HCFS_USER -c '/usr/bin/hadoop fs -mkdir /user/vagrant /user/root'
    su -s /bin/bash $HCFS_USER -c 'hadoop fs -chmod 777 /user/vagrant'
    su -s /bin/bash $HCFS_USER -c 'hadoop fs -chmod 777 /user/root'

    yum install -y pig hive flume mahout sqoop

    ./gradlew clean compileGroovy test -Dsmoke.tests=${SMOKE_TEST_COMPONENTS} --info
    HERE" > logs/smoke.tmp 2>&1 
    sed "s,\x1B\[[0-9;]*[a-zA-Z],,g" logs/smoke.tmp | tr -d '^M' > logs/smoke_tests.log 
else
    echo "Smoke tests did not run because run_smoke_tests set to false"
fi 

rm -f logs/*.tmp 
