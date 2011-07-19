#!/bin/bash

MAJOR_VERSION=${HADOOP_VERSION}
SRC_PKG=hadoop

namenode_user=hdfs
secondarynamenode_user=hdfs
datanode_user=hdfs
jobtracker_user=mapred
tasktracker_user=mapred

for node in namenode secondarynamenode jobtracker tasktracker datanode ; do
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

