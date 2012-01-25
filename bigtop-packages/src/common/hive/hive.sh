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
#
#
#
# Starts a Hive @HIVE_DAEMON@
#
# chkconfig: 345 90 10
# description: Starts a Hive @HIVE_DAEMON@
# processname: hive
# pidfile: /var/run/hive/hive-@HIVE_DAEMON@.pid
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
if [ -e /usr/libexec/bigtop-detect-javahome ]; then
  . /usr/libexec/bigtop-detect-javahome
elif [ -e /usr/lib/bigtop-utils/bigtop-detect-javahome ]; then
  . /usr/lib/bigtop-utils/bigtop-detect-javahome
fi

RETVAL_SUCCESS=0

STATUS_RUNNING=0
STATUS_DEAD=1
STATUS_DEAD_AND_LOCK=2
STATUS_NOT_RUNNING=3

ERROR_PROGRAM_NOT_INSTALLED=5
ERROR_PROGRAM_NOT_CONFIGURED=6

PATH=/usr/local/sbin:/usr/local/bin:/sbin:/bin:/usr/sbin:/usr/bin
NAME="hive-@HIVE_DAEMON@"
DESC="Hive @HIVE_DAEMON@ daemon"
SYS_FILE="/etc/default/${NAME}"
EXE_FILE="/usr/lib/hive/bin/hive"
PID_FILE="/var/run/hive/${NAME}.pid"
LOCKFILE="/var/lock/subsys/${NAME}"
LOG_FILE="/var/log/hive/${NAME}.log"
HIVE_USER="hive"
HIVE_HOME="`eval echo ~$HIVE_USER`"
NICENESS="+0"
TIMEOUT=3
USER="hive"

[ -f $SYS_FILE ] && . $SYS_FILE

hive_start() {
    [ -x $EXE_FILE ] || exit $ERROR_PROGRAM_NOT_INSTALLED

    service_name="@HIVE_DAEMON@"
    if [ $service_name = "server" ] ; then
      service_name="hiveserver"
      exec_env="HADOOP_OPTS=\"-Dhive.log.dir=`dirname $LOG_FILE`\""
    fi

    if [ -x /sbin/runuser ]; then
      SU="runuser -s /bin/bash $USER"
    else
      SU="su -s /bin/sh $USER"
    fi

    log_success_msg "Starting $desc (${NAME}): "
     $SU -c "cd $HIVE_HOME ; $exec_env nohup \ 
           $EXE_FILE --service $service_name $PORT \
             > $LOG_FILE 2>&1 < /dev/null & "'echo $! '"> $PID_FILE"

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
    status=$?

    case "$status" in
      $STATUS_RUNNING)
        log_success_msg "@HIVE_DAEMON@ is running"
        ;;
      $STATUS_DEAD)
        log_failure_msg "@HIVE_DAEMON@ is dead and pid file exists"
        ;;
      $STATUS_DEAD_AND_LOCK)
        log_failure_msg "@HIVE_DAEMON@ is dead and lock file exists"
        ;;
      $STATUS_NOT_RUNNING)
        log_failure_msg "@HIVE_DAEMON@ is not running"
        ;;
      *)
        log_failure_msg "@HIVE_DAEMON@ status is unknown"
        ;;
    esac
    return $status
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
