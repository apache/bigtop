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
# Starts a Flume @FLUME_DAEMON@
#
# chkconfig: 2345 90 10
# description:       Flume @FLUME_DAEMON@
# Provides:          flume-@FLUME_DAEMON@
# Required-Start:    $syslog $remote_fs
# Should-Start:
# Required-Stop:     $syslog $remote_fs
# Should-Stop:
# Default-Start:     2 3 4 5
# Default-Stop:      0 1 6
# Short-Description: Flume @FLUME_DAEMON@

. /etc/rc.d/init.d/functions

# Autodetect JAVA_HOME if not defined
if [ -e /usr/libexec/bigtop-detect-javahome ]; then
  . /usr/libexec/bigtop-detect-javahome
elif [ -e /usr/lib/bigtop-utils/bigtop-detect-javahome ]; then
  . /usr/lib/bigtop-utils/bigtop-detect-javahome
fi

desc="Flume @FLUME_DAEMON@ daemon"


export FLUME_LOG_DIR=/var/log/flume
export FLUME_HOME=/usr/lib/flume
export FLUME_RUN=/var/run/flume
export FLUME_PID=${FLUME_RUN}/flume-flume-@FLUME_DAEMON@.pid
export DOTIME=3
install -d -m 0755 -o flume -g flume ${FLUME_RUN}

checkstatus(){
  status -p ${FLUME_PID} flume
  RETVAL=$?
}

start() {
  echo -n $"Starting $desc (flume-@FLUME_DAEMON@): "

  su -s /bin/sh flume -c '${FLUME_HOME}/bin/flume-daemon.sh start @FLUME_DAEMON@'
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
  status)
    checkstatus
    ;;
  restart)
    restart
    ;;
  force-reload|condrestart|try-restart)
    checkstatus
    [ $RETVAL -eq 0 ] && restart
    ;;
  *)
    echo $"Usage: $0 {start|stop|restart|force-reload|condrestart|try-restart}"
    exit 1
esac

exit $RETVAL
