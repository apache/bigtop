#! /bin/bash
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

# skeleton  example file to build /etc/init.d/ scripts.
#    This file should be used to construct scripts for /etc/init.d.
#
#    Written by Miquel van Smoorenburg <miquels@cistron.nl>.
#    Modified for Debian
#    by Ian Murdock <imurdock@gnu.ai.mit.edu>.
#               Further changes by Javier Fernandez-Sanguino <jfs@debian.org>
#
# Starts a Hive @HIVE_DAEMON@
#
# chkconfig: 345 85 15
# description: Starts a Hive @HIVE_DAEMON@
# processname: hive
#
### BEGIN INIT INFO
# Provides:          hive-@HIVE_DAEMON@
# Required-Start:    $syslog $remote_fs
# Should-Start:
# Required-Stop:     $syslog $remote_fs
# Should-Stop:
# Default-Start:     3 4 5
# Default-Stop:      0 1 2 6
# Short-Description: Starts a Hive @HIVE_DAEMON@
### END INIT INFO

source /lib/lsb/init-functions

# Autodetect JAVA_HOME if not defined
. /usr/lib/bigtop-utils/bigtop-detect-javahome

RETVAL_SUCCESS=0

STATUS_RUNNING=0
STATUS_DEAD=1
STATUS_DEAD_AND_LOCK=2
STATUS_NOT_RUNNING=3
STATUS_DEBIAN_NOT_RUNNING=4

ERROR_PROGRAM_NOT_INSTALLED=5
ERROR_PROGRAM_NOT_CONFIGURED=6

PATH=/usr/local/sbin:/usr/local/bin:/sbin:/bin:/usr/sbin:/usr/bin
NAME="hive-@HIVE_DAEMON@"
DESC="Hive @HIVE_DAEMON@ daemon"
SYS_FILE="/etc/default/${NAME}"
EXE_FILE="/usr/lib/hive/bin/hive"
PID_FILE="/var/run/hive/${NAME}.pid"
LOCKFILE="/var/lock/subsys/${NAME}"
LOG_FILE="/var/log/hive/${NAME}.out"
HIVE_USER="hive"
HIVE_HOME="`eval echo ~$HIVE_USER`"
NICENESS="+0"
TIMEOUT=3

install -d -m 0755 -o ${HIVE_USER} -g ${HIVE_USER} `dirname ${PID_FILE}`

[ -f $SYS_FILE ] && . $SYS_FILE

hive_start() {
    [ -x $EXE_FILE ] || exit $ERROR_PROGRAM_NOT_INSTALLED

    exec_env="HADOOP_OPTS=\"-Dhive.log.dir=`dirname $LOG_FILE` -Dhive.log.file=${NAME}.log -Dhive.log.threshold=INFO\""
    service_name="@HIVE_DAEMON@"
    if [ $service_name = "server" ] ; then
      service_name="hiveserver"
    fi

    log_success_msg "Starting $desc (${NAME}): "
    /sbin/start-stop-daemon --quiet --oknodo --start --user $HIVE_USER --name java --background \
       --chuid $HIVE_USER --nicelevel $NICENESS --chdir $HIVE_HOME \
       --make-pidfile --pidfile $PID_FILE --startas /bin/sh -- \
       -c "$exec_env exec $EXE_FILE --service $service_name $PORT > $LOG_FILE 2>&1 < /dev/null"

    RETVAL=$?
    [ $RETVAL -eq $RETVAL_SUCCESS ] && touch $LOCKFILE
    return $RETVAL
}

hive_stop() {
    log_success_msg "Stopping $desc (${NAME}): "
    killproc -p $PID_FILE java
    RETVAL=$?

    [ $RETVAL -eq $RETVAL_SUCCESS ] && rm -f $LOCKFILE $PID_FILE
    return $RETVAL
}

hive_restart() {
    hive_stop
    [ -n "$TIMEOUT" ] && sleep $TIMEOUT
    hive_start
}

hive_status() {
    echo -n "Checking for service $desc: "
    pidofproc -p $PID_FILE java > /dev/null
    RETVAL=$?

    case "$RETVAL" in
      $STATUS_RUNNING)
        log_success_msg "@HIVE_DAEMON@ is running"
        ;;
      $STATUS_DEAD)
        log_failure_msg "@HIVE_DAEMON@ is dead and pid file exists"
        ;;
      $STATUS_DEAD_AND_LOCK)
        log_failure_msg "@HIVE_DAEMON@ is dead and lock file exists"
        ;;
      $STATUS_NOT_RUNNING|$STATUS_DEBIAN_NOT_RUNNING)
        log_failure_msg "@HIVE_DAEMON@ is not running"
        ;;
      *)
        log_failure_msg "@HIVE_DAEMON@ status is unknown"
        ;;
    esac
    return $RETVAL
}

RETVAL=0

case "$1" in
    start)
      hive_start
      ;;

    stop|force-stop)
      hive_stop
      ;; 

    force-reload|condrestart|try-restart)
      [ -e $LOCKFILE ] && hive_restart || :
      ;;

    restart|reload)
      hive_restart
      ;;
  
    status)
      hive_status
      ;;

    *)
	N=/etc/init.d/$NAME
	echo "Usage: $N {start|stop|restart|reload|condrestart|try-restart|force-reload|status|force-stop}" >&2

	exit 1
	;;
esac

exit $RETVAL
