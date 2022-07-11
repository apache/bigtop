echo -e "\033[32mSynchronizing script to ambari-server\033[0m"
docker exec ambari-server bash -c "mkdir -p /var/lib/ambari-agent/cache/stacks/BGTP/1.0/services/"
docker cp ../../../bgtp-ambari-mpack/src/main/resources/stacks/BGTP/1.0/services/ ambari-server:/var/lib/ambari-agent/cache/stacks/BGTP/1.0/

echo -e "\033[32mSynchronizing script to ambari-agent-01\033[0m"
docker exec ambari-agent-01 bash -c "mkdir -p /var/lib/ambari-agent/cache/stacks/BGTP/1.0/services/"
docker cp ../../../bgtp-ambari-mpack/src/main/resources/stacks/BGTP/1.0/services/ ambari-agent-01:/var/lib/ambari-agent/cache/stacks/BGTP/1.0/

echo -e "\033[32mSynchronizing script to ambari-agent-02\033[0m"
docker exec ambari-agent-02 bash -c "mkdir -p /var/lib/ambari-agent/cache/stacks/BGTP/1.0/services/"
docker cp ../../../bgtp-ambari-mpack/src/main/resources/stacks/BGTP/1.0/services/ ambari-agent-02:/var/lib/ambari-agent/cache/stacks/BGTP/1.0/

echo -e "\033[32mDone!\033[0m"