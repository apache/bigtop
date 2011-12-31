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
# chkconfig: 2345 82 13
# description: Summary: HBase is the Hadoop database. Use it when you need random, realtime read/write access to your Big Data. This project's goal is the hosting of very large tables -- billions of rows X millions of columns -- atop clusters of commodity hardware.
# processname: HBase
#
### BEGIN INIT INFO
# Provides:          hbase-@HBASE_DAEMON@
# Required-Start:    $network $local_fs $remote_fs
# Required-Stop:     $remote_fs
# Should-Start:      $named
# Should-Stop:
# Default-Start:     2 3 4 5
# Default-Stop:      0 1 6
# Short-Description: Hadoop HBase @HBASE_DAEMON@ daemon
### END INIT INFO

set -e

. /etc/default/hadoop

# Our default HBASE_HOME and HBASE_PID_DIR
export HBASE_HOME=/usr/lib/hbase
export HBASE_PID_DIR=/var/run/hbase

# Include HBase defaults if available
if [ -f /etc/default/hbase ] ; then
  . /etc/default/hbase
fi

if [ -f /usr/lib/hbase/bin/hbase-config.sh ] ; then
  . /usr/lib/hbase/bin/hbase-config.sh
fi

if [ -z "$HBASE_PID_DIR" -o -z "$HBASE_HOME" ]; then
  echo No HBASE_HOME or HBASE_PID_DIR set.
  exit 1
fi


install -d -m 0755 -o hbase -g hbase ${HBASE_PID_DIR}

PATH=/usr/local/sbin:/usr/local/bin:/sbin:/bin:/usr/sbin:/usr/bin
DAEMON_SCRIPT=$HBASE_HOME/bin/hbase-daemon.sh
NAME=hbase-@HBASE_DAEMON@
DESC="Hadoop HBase @HBASE_DAEMON@ daemon"
PID_FILE=$HBASE_PID_DIR/hbase-hbase-@HBASE_DAEMON@.pid


DODTIME=3                   # Time to wait for the server to die, in seconds
                            # If this value is set too low you might not
                            # let some servers to die gracefully and
                            # 'restart' will not work

# Checks if the given pid represents a live process.
# Returns 0 if the pid is a live process, 1 otherwise
hbase_is_process_alive() {
  local pid="$1" 
  ps -fp $pid | grep $pid | grep @HBASE_DAEMON@ > /dev/null 2>&1
}

# Check if the process associated to a pidfile is running.
# Return 0 if the pidfile exists and the process is running, 1 otherwise
hbase_check_pidfile() {
  local pidfile="$1" # IN
  local pid

  pid=`cat "$pidfile" 2>/dev/null`
  if [ "$pid" = '' ]; then
    # The file probably does not exist or is empty. 
    return 1
  fi
  
  set -- $pid
  pid="$1"

  hbase_is_process_alive $pid
}

# Kill the process associated to a pidfile
hbase_stop_pidfile() {
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
   if hbase_process_kill "$pid" 15; then
      return 0
   fi

   # Otherwise try the hard way
   if hbase_process_kill "$pid" 9; then
      return 0
   fi

   return 1
}

hbase_process_kill() {
    local pid="$1"    # IN
    local signal="$2" # IN
    local second

    kill -$signal $pid 2>/dev/null

   # Wait a bit to see if the dirty job has really been done
    for second in 0 1 2 3 4 5 6 7 8 9 10; do
        if hbase_is_process_alive "$pid"; then
         # Success
            return 0
        fi

        sleep 1
    done

   # Timeout
    return 1
}


start() {
    su -s /bin/sh hbase -c "$DAEMON_SCRIPT start @HBASE_DAEMON@"
}
stop() {
    su -s /bin/sh hbase -c "$DAEMON_SCRIPT stop @HBASE_DAEMON@"
}


case "$1" in
  start)
        echo -n "Starting $DESC: "
        start
        if hbase_check_pidfile $PID_FILE ; then
            echo "$NAME."
        else
            echo "ERROR."
        fi
  ;;
  stop)
        echo -n "Stopping $DESC: "
        stop
        if hbase_check_pidfile $PID_FILE ; then
            echo 'ERROR'
        else
            echo "$NAME."
        fi
  ;;
  force-stop)
        echo -n "Forcefully stopping $DESC: "
        hbase_stop_pidfile $PID_FILE
        if hbase_check_pidfile $PID_FILE ; then
            echo "$NAME."
        else
            echo " ERROR."
        fi
  ;;
  force-reload)
  # check wether $DAEMON is running. If so, restart
        hbase_check_pidfile $PID_FILE && $0 restart
  ;;
  restart)
        echo -n "Restarting $DESC: "
        stop
        [ -n "$DODTIME" ] && sleep $DODTIME
        $0 start
  ;;
  status)
    echo -n "$NAME is "
    if hbase_check_pidfile $PID_FILE ;  then
        echo "running"
    else
        echo "not running."
        exit 1
    fi
    ;;
  *)
  N=/etc/init.d/$NAME
  # echo "Usage: $N {start|stop|restart|reload|force-reload}" >&2
  echo "Usage: $N {start|stop|restart|force-reload|status|force-stop}" >&2
  exit 1
  ;;
esac

exit 0
