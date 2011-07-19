#!/bin/sh
#
#  Licensed to Cloudera, Inc. under one or more contributor license
#  agreements.  See the NOTICE file distributed with this work for
#  additional information regarding copyright ownership.  Cloudera,
#  Inc. licenses this file to you under the Apache License, Version
#  2.0 (the "License"); you may not use this file except in compliance
#  with the License.  You may obtain a copy of the License at
#
#  http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

#
# Copyright (c) 2011 Cloudera, inc.
#

#
# Resolve Oozie install directory
#

# $0 may be a softlink
PRG="${0}"
while [ -h "${PRG}" ]; do
  ls=`ls -ld "${PRG}"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "${PRG}"`/"$link"
  fi
done

OOZIE_DIR=`dirname ${PRG}`
OOZIE_DIR=`cd ${OOZIE_DIR}/..;pwd`


failIfNotOK() {
  if [ $? != 0 ]; then
    echo $1
    if [ -f "${WORKDIR}" ]; then
      rm -rf ${WORKDIR}
    fi
    exit $?
  fi
}

remove_examples() {
  hadoop fs -rmr examples
  failIfNotOK "Deletion of examples from HDFS failed"
}

install_examples() {
  EXAMPLESTARFILE="${OOZIE_DIR}/oozie-examples.tar.gz"
  if [ ! -f "${EXAMPLESTARFILE}" ]; then
    EXAMPLESTARFILE="/usr/share/doc/oozie/oozie-examples.tar.gz"
  fi
  if [ ! -f "${EXAMPLESTARFILE}" ]; then
    EXAMPLESTARFILE="/usr/share/doc/packages/oozie/oozie-examples.tar.gz"
  fi
  if [ ! -f "${EXAMPLESTARFILE}" ]; then
    echo "Could not find Oozie examples tarball"
    exit 1
  fi

  WORKDIR=`mktemp -d /tmp/oozie.XXXXXX`
  failIfNotOK "Could not create temp dir"
  cd ${WORKDIR}
  tar xzf ${EXAMPLESTARFILE}
  failIfNotOK "Could not expand examples"
  hadoop fs -put examples examples
  failIfNotOK "Could not copy examples to HDFS"
  rm -rf ${WORKDIR}
}

if [ "${1}" = "-install" ]; then
  hadoop fs -test -e examples
  if [ $? = 0 ]; then
    remove_examples
  fi
  install_examples
elif [ "${1}" = "-remove" ]; then
  remove_examples
else
  echo "Usage: -install|-remove"
  exit 1
fi
