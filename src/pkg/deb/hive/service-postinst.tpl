#!/bin/sh
# postinst script for hive
#
# see: dh_installdeb(1)

set -e

case "$1" in
    configure)
        update-rc.d hadoop-hive-@HIVE_DAEMON@ defaults >/dev/null || exit 1
    ;;

    abort-upgrade|abort-remove|abort-deconfigure)
    ;;

    *)
        echo "postinst called with unknown argument \`$1'" >&2
        exit 1
    ;;
esac

exit 0


