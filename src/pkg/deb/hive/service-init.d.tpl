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
# Version:  @(#)skeleton  1.9  26-Feb-2001  miquels@cistron.nl
#
### BEGIN INIT INFO
# Provides:          hive-@HIVE_DAEMON@
# Required-Start:    $network $local_fs $remote_fs
# Required-Stop:     $remote_fs
# Should-Start:      $named
# Should-Stop:
# Default-Start:     2 3 4 5
# Default-Stop:      0 1 6
# Short-Description: hive
### END INIT INFO

# Modelled after $HADOOP_HOME/bin/hadoop-daemon.sh

PATH=/usr/local/sbin:/usr/local/bin:/sbin:/bin:/usr/sbin:/usr/bin
SYS_FILE="/etc/default/hadoop-hive-@HIVE_DAEMON@"
EXE_FILE="/usr/lib/hive/bin/hive"
PID_FILE="/var/run/hive/hive-@HIVE_DAEMON@.pid"
LOG_FILE="/var/log/hive/hive-@HIVE_DAEMON@.log"
HIVE_USER="hive"
NICENESS="0"
NAME="hadoop-hive-@HIVE_DAEMON@"
DESC="Hive daemon"
 
DODTIME=3
SLAVE_TIMEOUT=300

[ -f $SYS_FILE ] && . $SYS_FILE

hive_die() {
    echo "$@"
    exit 1
}
hive_is_process_alive() {
    local pid="$1"
    kill -0 $pid > /dev/null 2>&1
}
hive_check_pidfile() {
    local pidfile="$1" # IN
    local pid

    pid=`cat "$pidfile" 2>/dev/null`
    if [ "$pid" = '' ]; then
    # The file probably does not exist or is empty. 
	return 1
    fi
    
    set -- $pid
    pid="$1"

    hive_is_process_alive $pid
}
hive_process_kill() {
    local pid="$1"    # IN
    local signal="$2" # IN
    local second

    kill -$signal $pid 2>/dev/null

    for second in 0 1 2 3 4 5 6 7 8 9 10; do
      hive_is_process_alive "$pid" || return 0
      sleep 1
    done

    return 1
}
hive_stop_pidfile() {
    local pidfile="$1" # IN
    local pid

    pid=`cat "$pidfile" 2>/dev/null`
    if [ "$pid" = '' ]; then
      # The file probably does not exist or is empty. Success
	return 0
    fi
    
    set -- $pid
    pid="$1"

    # First try the easy way
    if hive_process_kill "$pid" 15; then
	return 0
    fi

    # Otherwise try the hard way
    if hive_process_kill "$pid" 9; then
	return 0
    fi

    return 1
}

hive_start() {
    if hive_check_pidfile $PID_FILE ; then
      exit 0
    fi

    service_name="@HIVE_DAEMON@"
    if [ $service_name = "server" ] ; then
      service_name="hiveserver"
      exec_env="HADOOP_OPTS=\"-Dhive.log.dir=`dirname $LOG_FILE`\""
    fi
    echo starting $EXE_FILE, logging to $LOG_FILE
    su -s /bin/sh $HIVE_USER \
       -c "$exec_env nohup nice -n $NICENESS       \
           $EXE_FILE --service $service_name $PORT \
             > $LOG_FILE 2>&1 < /dev/null & "'echo $! '"> $PID_FILE"
    sleep 3

    hive_check_pidfile $PID_FILE || hive_die "failed to start @HIVE_DAEMON@"
}
hive_stop() {
    if [ -f $PID_FILE ]; then
      hive_stop_pidfile $PID_FILE || hive_die "failed to stop metastore"
      rm $PID_FILE  
    fi
}
hive_restart() {
    hive_stop
    [ -n "$DODTIME" ] && sleep $DODTIME
    hive_start
}
hive_status() {
    echo -n "$NAME is "
    if hive_check_pidfile $PID_FILE ;  then
     echo "running"
    else
     echo "not running"
     exit 1
    fi
}

case "$1" in
    start)
      hive_start
      ;;

    stop|force-stop)
      hive_stop
      ;; 

    force-reload|condrestart|try-restart)
      hive_check_pidfile $PID_FILE && hive_restart
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
