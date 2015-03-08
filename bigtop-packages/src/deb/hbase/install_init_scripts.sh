#!/bin/bash
#
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

SRC_PKG=hbase
for node in master regionserver rest thrift thrift2; do
    service_pkgdir=debian/$SRC_PKG-$node
    if [ "$node" == "regionserver" ] ; then
        # Region servers start from a different template that allows
        # them to run multiple concurrent instances of the daemon
        template=debian/regionserver-init.d.tpl
        sed -i -e "s|@INIT_DEFAULT_START@|2 3 4 5|" $template
        sed -i -e "s|@INIT_DEFAULT_STOP@|0 1 6|" $template
        sed -e "s|@HBASE_DAEMON@|$node|" -e "s|@CHKCONFIG@|2345 87 13|" $template > debian/hbase-$node.init
    else
        sed -e "s|@HBASE_DAEMON@|$node|" debian/hbase.svc > debian/hbase-$node.svc
	bash debian/init.d.tmpl debian/hbase-$node.svc deb debian/hbase-$node.init 
    fi

    mkdir -p $service_pkgdir/usr/share/lintian/overrides
    echo "$SRC_PKG-$node: new-package-should-close-itp-bug" > $service_pkgdir/usr/share/lintian/overrides/$SRC_PKG-$node

done

