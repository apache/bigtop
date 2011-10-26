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
### BEGIN INIT INFO
# Provides:             flume-@FLUME_DAEMON@
# Required-Start:       $local_fs $remote_fs $syslog $named $network $time
# Required-Stop:        $local_fs $remote_fs $syslog $named $network
# Default-Start:        2 3 4 5
# Default-Stop:         0 1 6
# Short-Description:    Apache Flume @FLUME_DAEMON@
### END INIT INFO
# Starts a Flume @FLUME_DAEMON@
#
# description: Flume @FLUME_DAEMON@

# Autodetect JAVA_HOME if not defined
if [ -e /usr/libexec/bigtop-detect-javahome ]; then
  source /usr/libexec/bigtop-detect-javahome
elif [ -e /usr/lib/bigtop-utils/bigtop-detect-javahome ]; then
  source /usr/lib/bigtop-utils/bigtop-detect-javahome
fi

export FLUME_HOME=/usr/lib/flume
export FLUME_LOG_DIR=/var/log/flume
export FLUME_RUN=/var/run/flume
export FLUME_PID=${FLUME_RUN}/flume-flume-@FLUME_DAEMON@.pid
export DOTIME=3
install -d -m 0755 -o flume -g flume ${FLUME_RUN}

desc="Flume @FLUME_DAEMON@ daemon"


# Checks if the given pid represents a live process.
# Returns 0 if the pid is a live process, 1 otherwise
flume_is_process_alive() {
  local pid="$1"
  ps -fp $pid | grep $pid | grep flume > /dev/null 2>&1
}

# Check if the process associated to a pidfile is running.
# Return 0 if the pidfile exists and the process is running, 1 otherwise
flume_check_pidfile() {
  local pidfile="$1" # IN
  local pid

  pid=`cat "$pidfile" 2>/dev/null`
  if [ "$pid" = '' ]; then
    # The file probably does not exist or is empty.
    return 1
  fi

  set -- $pid
  pid="$1"

  flume_is_process_alive $pid
}


start() {
  echo -n $"Starting $desc (flume-@FLUME_DAEMON@): "
  su -s /bin/sh  flume -c '${FLUME_HOME}/bin/flume-daemon.sh start @FLUME_DAEMON@'
  echo
}

stop() {
  echo -n $"Stopping $desc (flume-@FLUME_DAEMON@): "
  su -s /bin/sh  flume -c '${FLUME_HOME}/bin/flume-daemon.sh stop @FLUME_DAEMON@'
  [ $? -eq 0 ] && rm -f $FLUME_PID
  echo
}

restart() {
  stop
  echo "Sleeping for ${DOTIME}"
  sleep ${DOTIME}
  start
}

case "$1" in
  start)
    start
    ;;
  stop)
    stop
    ;;
  restart|force-reload)
    restart
    ;;
  status)
    echo -n "${desc} is "
    if flume_check_pidfile $FLUME_PID ;  then
      echo "running"
      RETVAL=0
    else
      echo "not running."
      RETVAL=3
    fi
    ;;
  *)
    echo $"Usage: $0 {start|stop|restart}"
    exit 1
esac

exit $RETVAL
