#!/bin/bash

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

# chkconfig: 2345 90 10
# description: Sqoop allows easy imports and exports of data sets between \
# databases and the Hadoop Distributed File System (HDFS). The Sqoop \
# metastore allows users to define saved jobs for repeated execution and \
# share them with other users of the cluster.
# processname: java
# pidfile: /var/run/sqoop/sqoop-metastore.pid
### BEGIN INIT INFO
# Provides:          Sqoop
# Required-Start:    $network $local_fs $remote_fs
# Required-Stop:     $remote_fs
# Should-Start:      $named
# Should-Stop:
# Default-Start:     2 3 4 5
# Default-Stop:      0 1 6
# Short-Description: Sqoop allows easy imports and exports of data sets between databases and the Hadoop Distributed File System (HDFS).
### END INIT INFO
set -e

# Autodetect JAVA_HOME if not defined
. /usr/lib/bigtop-utils/bigtop-detect-javahome

BIGTOP_DEFAULTS_DIR=${BIGTOP_DEFAULTS_DIR-/etc/default}
[ -n "${BIGTOP_DEFAULTS_DIR}" -a -r ${BIGTOP_DEFAULTS_DIR}/hadoop ] && . ${BIGTOP_DEFAULTS_DIR}/hadoop

PATH=/usr/local/sbin:/usr/local/bin:/sbin:/bin:/usr/sbin:/usr/bin

NAME=sqoop-metastore
DESC="Sqoop metastore"
PID_FILE=/var/run/sqoop/sqoop-metastore.pid
LOGDIR=/var/log/sqoop

DODTIME=3

# Returns 0 if pid is alive, 1 if not.
hadoop_is_process_alive() {
  local pid="$1"
  ps -fp $pid | grep $pid | grep sqoop > /dev/null 2>&1
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
    for second in {0..10}; do
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
    # Pid files created in sqoop-specific directory under /var/run.
    # The dir should be recreated first.
    local piddir=`dirname "$PID_FILE"`
    install -d -m 0755 -o sqoop -g sqoop "$piddir"
    runuser -s /bin/bash sqoop -c \
         "/usr/lib/sqoop/bin/start-metastore.sh -p $PID_FILE -l $LOGDIR"
}
stop() {
    runuser -s /bin/bash sqoop -c \
        "/usr/lib/sqoop/bin/stop-metastore.sh -p $PID_FILE"
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
            echo "ERROR."
        fi
        rm $PID_FILE
        ;;
    force-reload|condrestart|try-restart)
        # check whether $DAEMON is running. If so, restart
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
        echo "Usage: $N {start|stop|restart|force-reload|status|force-stop|condrestart|try-restart}" >&2
        exit 1
        ;;
esac

exit 0
