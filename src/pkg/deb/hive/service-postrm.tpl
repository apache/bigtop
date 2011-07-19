#!/bin/sh
# postrm script for hive
#
# see: dh_installdeb(1)

set -e

case "$1" in
    purge)
        update-rc.d -f hadoop-hive-@HIVE_DAEMON@ remove > /dev/null || exit 1
    ;;
    upgrade)
        service hadoop-hive-@HIVE_DAEMON@ condrestart >/dev/null || :
    ;;
    remove|failed-upgrade|abort-install|abort-upgrade|disappear)
    ;;

    *)
        echo "postrm called with unknown argument \`$1'" >&2
        exit 1
    ;;
esac

exit 0


