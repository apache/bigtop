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
# chkconfig: 2345 82 13
# description: Summary: HBase is the Hadoop database. Use it when you need random, realtime read/write access to your Big Data. This project's goal is the hosting of very large tables -- billions of rows X millions of columns -- atop clusters of commodity hardware.
# processname: HBase
# pidfile: /usr/lib/hbase/pids/hadoop_hbase.pid
### BEGIN INIT INFO
# Provides:          HBase
# Required-Start:    $network $local_fs
# Required-Stop:
# Should-Start:      $named
# Should-Stop:
# Default-Start:     2 3 4 5
# Default-Stop:      0 1 6
# Short-Description: HBase
### END INIT INFO
set -e

. /etc/default/hadoop
. /etc/default/hbase

# Autodetect JAVA_HOME if not defined
if [ -e /usr/libexec/bigtop-detect-javahome ]; then
  . /usr/libexec/bigtop-detect-javahome
elif [ -e /usr/lib/bigtop-utils/bigtop-detect-javahome ]; then
  . /usr/lib/bigtop-utils/bigtop-detect-javahome
fi

PATH=/usr/local/sbin:/usr/local/bin:/sbin:/bin:/usr/sbin:/usr/bin
DAEMON_SCRIPT="/usr/lib/hbase/bin/hbase-daemon.sh"
NAME=HBase
DESC="HBase daemon"
 
if [ -f /usr/lib/hbase/bin/hbase-config.sh ] ; then
  . /usr/lib/hbase/bin/hbase-config.sh
fi
install -d -m 0755 -o hbase -g hbase ${HBASE_PID_DIR}

PID_FILE=${HBASE_PID_DIR}/hbase-hbase-@HBASE_DAEMON@.pid



DODTIME=3


hadoop_is_process_alive() {
    local pid="$1"
    ps -fp $pid | grep $pid | grep -i @HBASE_DAEMON@ > /dev/null 2>&1
}

hadoop_check_pidfile() {
    local pidfile="$1" # IN
    local pid

    pid=`cat "$pidfile" 2>/dev/null`
    if [ "$pid" = '' ]; then
    # The file probably does not exist or is empty.
	return 1
    fi

    set -- $pid
    pid="$1"

    hadoop_is_process_alive $pid
}
hadoop_check_pidfile() {
    local pidfile="$1" # IN
    local pid

    pid=`cat "$pidfile" 2>/dev/null`
    if [ "$pid" = '' ]; then
    # The file probably does not exist or is empty. 
	return 1
    fi
    
    set -- $pid
    pid="$1"

    hadoop_is_process_alive $pid
}

hadoop_process_kill() {
    local pid="$1"    # IN
    local signal="$2" # IN
    local second

    kill -$signal $pid 2>/dev/null

   # Wait a bit to see if the dirty job has really been done
    for second in 0 1 2 3 4 5 6 7 8 9 10; do
	if hadoop_is_process_alive "$pid"; then
         # Success
	    return 0
	fi

	sleep 1
    done

   # Timeout
    return 1
}
hadoop_stop_pidfile() {
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
    if hadoop_process_kill "$pid" 15; then
	return 0
    fi

   # Otherwise try the hard way
    if hadoop_process_kill "$pid" 9; then
	return 0
    fi

    return 1
}


start() {
    su -s /bin/sh hbase -c "${DAEMON_SCRIPT} start @HBASE_DAEMON@"
}
stop() {
    su -s /bin/sh hbase -c "${DAEMON_SCRIPT} stop @HBASE_DAEMON@"
}

case "$1" in
    start)
	start
	;;
    stop)
	stop 
	;;
    force-stop)
        echo -n "Forcefully stopping $DESC: "
        hadoop_stop_pidfile $PID_FILE
        if ! hadoop_check_pidfile $PID_FILE ; then
            echo "$NAME."
        else
            echo " ERROR."
        fi
	;;
    force-reload|condrestart|try-restart)
  # check wether $DAEMON is running. If so, restart
        hadoop_check_pidfile $PID_FILE && $0 restart
	;;
    restart|reload)
        echo -n "Restarting $DESC: "
        stop
        [ -n "$DODTIME" ] && sleep $DODTIME
        $0 start
	;;
    status)
	echo -n "$NAME is "
	if hadoop_check_pidfile $PID_FILE ;  then
	    echo "running"
	else
	    echo "not running."
	    exit 1
	fi
	;;

    *)
	N=/etc/init.d/$NAME
  # echo "Usage: $N {start|stop|restart|reload|force-reload}" >&2
	echo "Usage: $N {start|stop|restart|force-reload|status|force-stop|condrestart|try-restart}" >&2

	exit 1
	;;
esac

exit 0
