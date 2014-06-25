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

for node in master tserver gc monitor tracer; do
    service_pkgdir=debian/accumulo-$node
    debdir=$service_pkgdir/DEBIAN
    mkdir -p $service_pkgdir/etc/init.d/ $debdir
    bash debian/init.d.tmpl debian/accumulo-$node.svc deb $service_pkgdir/etc/init.d/accumulo-$node
    bash $RPM_SOURCE_DIR/init.d.tmpl $RPM_SOURCE_DIR/accumulo-$node.svc rpm $init_file
    sed -e "s|@ACCUMULO_DAEMON@|$node|" debian/service-postinst.tpl > $debdir/postinst
    sed -e "s|@ACCUMULO_DAEMON@|$node|" debian/service-postrm.tpl > $debdir/postrm
    echo /etc/init.d/accumulo-$node > $debdir/conffiles
    chmod 755 $debdir/postinst $debdir/postrm $service_pkgdir/etc/init.d*

    mkdir -p $service_pkgdir/usr/share/lintian/overrides
    echo "accumulo-$node: new-package-should-close-itp-bug" > $service_pkgdir/usr/share/lintian/overrides/accumulo-$node

done

