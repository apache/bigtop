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

MAJOR_VERSION=${HADOOP_VERSION}
SRC_PKG=hadoop

namenode_user=hdfs
secondarynamenode_user=hdfs
datanode_user=hdfs
jobtracker_user=yarn
tasktracker_user=yarn

for node in namenode secondarynamenode datanode resourcemanager nodemanager; do
    service_pkgdir=debian/$SRC_PKG-$node
    debdir=$service_pkgdir/DEBIAN
    template="debian/service-init.d.tpl"
    user=$(eval "echo \$${node}_user")
    mkdir -p $service_pkgdir/etc/init.d/ $debdir
    sed -e "s|@HADOOP_DAEMON@|$node|" -e "s|@HADOOP_MAJOR_VERSION@|$MAJOR_VERSION|" \
	-e "s|@DAEMON_USER@|$user|" \
        $template > $service_pkgdir/etc/init.d/$SRC_PKG-$node
    sed -e "s|@HADOOP_DAEMON@|$node|" -e "s|@HADOOP_MAJOR_VERSION@|$MAJOR_VERSION|" \
	-e "s|@DAEMON_USER@|$user|" \
        debian/service-postinst.tpl > $debdir/postinst
    sed -e "s|@HADOOP_DAEMON@|$node|" -e "s|@HADOOP_MAJOR_VERSION@|$MAJOR_VERSION|" \
	-e "s|@DAEMON_USER@|$user|" \
        debian/service-postrm.tpl > $debdir/postrm
    chmod 755 $service_pkgdir/etc/init.d/* $debdir/postinst $debdir/postrm

    # We aren't making packages for debian itself, so override ITP lintian warnings
    mkdir -p $service_pkgdir/usr/share/lintian/overrides
    echo "$SRC_PKG-$node: new-package-should-close-itp-bug" > $service_pkgdir/usr/share/lintian/overrides/$SRC_PKG-$node

done

