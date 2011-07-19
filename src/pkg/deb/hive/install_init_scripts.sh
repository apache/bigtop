#!/bin/sh

SRC_PKG=hadoop-hive
for node in server metastore ; do
    service_pkgdir=debian/$SRC_PKG-$node
    debdir=$service_pkgdir/DEBIAN
    template="debian/service-init.d.tpl"

    mkdir -p $service_pkgdir/etc/init.d/ $service_pkgdir/etc/default/ $debdir
    sed -e "s|@HIVE_DAEMON@|$node|" $template > $service_pkgdir/etc/init.d/$SRC_PKG-$node
    sed -e "s|@HIVE_DAEMON@|$node|" debian/hadoop-hive.default > $service_pkgdir/etc/default/$SRC_PKG-$node 
    sed -e "s|@HIVE_DAEMON@|$node|" debian/service-postinst.tpl > $debdir/postinst
    sed -e "s|@HIVE_DAEMON@|$node|" debian/service-postrm.tpl > $debdir/postrm
    chmod 755 $debdir/postinst $debdir/postrm $service_pkgdir/etc/init.d*

    mkdir -p $service_pkgdir/usr/share/lintian/overrides
    echo "$SRC_PKG-$node: new-package-should-close-itp-bug" > $service_pkgdir/usr/share/lintian/overrides/$SRC_PKG-$node

done
