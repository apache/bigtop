#!/bin/bash

# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

echo -e "\033[32mCreating network ambari\033[0m"
docker network create --driver bridge ambari

echo -e "\033[32mCreating container ambari-server\033[0m"
docker run -d -p 3306:3306 -p 5005:5005 -p 8080:8080 --name ambari-server --hostname ambari-server --network ambari --privileged -e "container=docker" -v /sys/fs/cgroup:/sys/fs/cgroup:ro ambari:2.7.5 /usr/sbin/init
SERVER_PUB_KEY=`docker exec ambari-server /bin/cat /root/.ssh/id_rsa.pub`
docker exec ambari-server bash -c "echo '$SERVER_PUB_KEY' > /root/.ssh/authorized_keys"
docker exec ambari-server /bin/systemctl enable sshd
docker exec ambari-server /bin/systemctl start sshd

echo -e "\033[32mSetting up mariadb-server\033[0m"
docker exec ambari-server /bin/systemctl enable mariadb
docker exec ambari-server /bin/systemctl start mariadb
docker exec ambari-server bash -c "mysql -e \"UPDATE mysql.user SET Password = PASSWORD('root') WHERE User = 'root'\""
docker exec ambari-server bash -c "mysql -e \"GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' IDENTIFIED BY 'root' WITH GRANT OPTION\""
docker exec ambari-server bash -c "mysql -e \"DROP USER ''@'localhost'\""
docker exec ambari-server bash -c "mysql -e \"DROP USER ''@'ambari-server'\""
docker exec ambari-server bash -c "mysql -e \"DROP DATABASE test\""
docker exec ambari-server bash -c "mysql -e \"CREATE DATABASE ambari\""
docker exec ambari-server bash -c "mysql --database=ambari -e  \"source /var/lib/ambari-server/resources/Ambari-DDL-MySQL-CREATE.sql\""
docker exec ambari-server bash -c "mysql -e \"FLUSH PRIVILEGES\""

echo -e "\033[32mSetting up ambari-server\033[0m"
docker exec ambari-server bash -c "ambari-server setup --java-home=/usr/lib/jvm/java --database=mysql --databasehost=localhost --databaseport=3306 --databasename=ambari --databaseusername=root --databasepassword=root -s"

echo -e "\033[32mCreating container ambari-agent-01\033[0m"
docker run -d --name ambari-agent-01 --hostname ambari-agent-01 --network ambari --privileged -e "container=docker" -v /sys/fs/cgroup:/sys/fs/cgroup:ro ambari:2.7.5 /usr/sbin/init
docker exec ambari-agent-01 bash -c "echo '$SERVER_PUB_KEY' > /root/.ssh/authorized_keys"
docker exec ambari-agent-01 /bin/systemctl enable sshd
docker exec ambari-agent-01 /bin/systemctl start sshd

echo -e "\033[32mCreating container ambari-agent-02\033[0m"
docker run -d --name ambari-agent-02 --hostname ambari-agent-02 --network ambari --privileged -e "container=docker" -v /sys/fs/cgroup:/sys/fs/cgroup:ro ambari:2.7.5 /usr/sbin/init
docker exec ambari-agent-02 bash -c "echo '$SERVER_PUB_KEY' > /root/.ssh/authorized_keys"
docker exec ambari-agent-02 /bin/systemctl enable sshd
docker exec ambari-agent-02 /bin/systemctl start sshd

echo -e "\033[32mConfiguring hosts file\033[0m"
AMBARI_SERVER_IP=`docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' ambari-server`
AMBARI_AGENT_01_IP=`docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' ambari-agent-01`
AMBARI_AGENT_02_IP=`docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' ambari-agent-02`
docker exec ambari-server bash -c "echo '$AMBARI_AGENT_01_IP      ambari-agent-01' >> /etc/hosts"
docker exec ambari-server bash -c "echo '$AMBARI_AGENT_02_IP      ambari-agent-02' >> /etc/hosts"
docker exec ambari-agent-01 bash -c "echo '$AMBARI_SERVER_IP      ambari-server' >> /etc/hosts"
docker exec ambari-agent-01 bash -c "echo '$AMBARI_AGENT_02_IP      ambari-agent-02' >> /etc/hosts"
docker exec ambari-agent-02 bash -c "echo '$AMBARI_SERVER_IP      ambari-server' >> /etc/hosts"
docker exec ambari-agent-02 bash -c "echo '$AMBARI_AGENT_01_IP      ambari-agent-01' >> /etc/hosts"


echo -e "\033[32mConfiguring Kerberos\033[0m"
docker cp ./krb5.conf ambari-server:/etc/krb5.conf
docker cp ./krb5.conf ambari-agent-01:/etc/krb5.conf
docker cp ./krb5.conf ambari-agent-02:/etc/krb5.conf
docker exec ambari-server bash -c "echo -e \"admin\nadmin\" | kdb5_util create -s -r EXAMPLE.COM"
docker exec ambari-server bash -c "echo -e \"admin\nadmin\" | kadmin.local -q \"addprinc admin/admin\""
docker exec ambari-server bash -c "systemctl start krb5kdc"
docker exec ambari-server bash -c "systemctl enable krb5kdc"
docker exec ambari-server bash -c "systemctl start kadmin"
docker exec ambari-server bash -c "systemctl enable kadmin"
# KDC HOST: ambari-server
# REALM NAME: EXAMPLE.COM
# ADMIN PRINCIPAL: admin/admin@EXAMPLE.COM
# ADMIN PASSWORD: admin

echo -e "\033[32mInstalling Bigtop Ambari Mpack\033[0m"

MVN_BIN=$(command -v mvn || exit 1)
if [[ -e $MVN_BIN ]]; then
  echo -e "\033[32mMaven found, Installing Mpack by Maven\033[0m"
  mvn clean install -DskipTests -Drat.skip -f ../../../bgtp-ambari-mpack/pom.xml
  docker cp ../../../bgtp-ambari-mpack/target/bgtp-ambari-mpack-1.0.0.0-SNAPSHOT-bgtp-ambari-mpack.tar.gz ambari-server:/
else
  echo -e "\033[32mMaven not found, Installing Mpack by Bigtop trunk container\033[0m"
  ARCH=`uname -m`
  basearch=
  if [ ${ARCH} == "aarch64" ]; then
    basearch="-aarch64"
  elif [ ${ARCH} == "ppc64le" ]; then
    basearch="-ppc64le"
  fi

  cd ../../../../../../../
  docker run --rm -v `pwd`:/ws --workdir /ws bigtop/slaves:trunk-centos-7${basearch} bash -c '. /etc/profile.d/bigtop.sh; ./gradlew bigtop-utils-clean bigtop-ambari-mpack-clean bigtop-utils-pkg bigtop-ambari-mpack-pkg'
  docker cp output/bigtop-ambari-mpack/noarch/bigtop-ambari-mpack-2.7.5.0-*.rpm ambari-server:/
  docker cp output/bigtop-utils/noarch/bigtop-utils-*.rpm ambari-server:/
  cd -

  docker exec ambari-server bash -c "rpm -ivh -f /bigtop-utils-*.rpm; rpm -ivh -f /bigtop-ambari-mpack-2.7.5.0-1.el7.noarch.rpm"
  docker exec ambari-server bash -c "cp /usr/lib/bigtop-ambari-mpack/bgtp-ambari-mpack-1.0.0.0-SNAPSHOT-bgtp-ambari-mpack.tar.gz /"
fi

docker exec ambari-server bash -c "ambari-server install-mpack --mpack=/bgtp-ambari-mpack-1.0.0.0-SNAPSHOT-bgtp-ambari-mpack.tar.gz"
docker exec ambari-server bash -c "ambari-server restart --debug"

echo -e "\033[32mPrint Ambari Server RSA Private Key\033[0m"
docker exec ambari-server bash -c "cat ~/.ssh/id_rsa"