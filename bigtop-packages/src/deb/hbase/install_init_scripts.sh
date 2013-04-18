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
for node in master regionserver rest thrift ; do
    case $service in
        master) chkconfig="2345 85 15" ;;
        thrift) chkconfig="2345 86 14" ;;
        regionserver) chkconfig="2345 87 13" ;;
        rest) chkconfig="2345 88 12" ;;
        *) chkconfig="2345 89 13" ;;
    esac
    service_pkgdir=debian/$SRC_PKG-$node
    debdir=$service_pkgdir/DEBIAN
    template="debian/service-init.d.tpl"
    if [ "$node" == "regionserver" ] ; then
        # Region servers start from a different template that allows
        # them to run multiple concurrent instances of the daemon
        template="debian/regionserver-init.d.tpl"
        sed -i -e "s|@INIT_DEFAULT_START@|2 3 4 5|" $template
        sed -i -e "s|@INIT_DEFAULT_STOP@|0 1 6|" $template
    fi

    mkdir -p $service_pkgdir/etc/init.d/ $debdir
    sed -e "s|@HBASE_DAEMON@|$node|" -e "s|@CHKCONFIG@|$chkconfig|" $template > $service_pkgdir/etc/init.d/$SRC_PKG-$node
    sed -e "s|@HBASE_DAEMON@|$node|" debian/service-postinst.tpl > $debdir/postinst
    sed -e "s|@HBASE_DAEMON@|$node|" debian/service-postrm.tpl > $debdir/postrm
    echo /etc/init.d/$SRC_PKG-$node > $debdir/conffiles
    chmod 755 $debdir/postinst $debdir/postrm $service_pkgdir/etc/init.d*

    mkdir -p $service_pkgdir/usr/share/lintian/overrides
    echo "$SRC_PKG-$node: new-package-should-close-itp-bug" > $service_pkgdir/usr/share/lintian/overrides/$SRC_PKG-$node

done

# FIXME: BIGTOP-648 workaround for HBASE-6263
sed -i -e 's# start thrift"# start thrift $HBASE_THRIFT_MODE"#' debian/hbase-thrift/etc/init.d/hbase-thrift
