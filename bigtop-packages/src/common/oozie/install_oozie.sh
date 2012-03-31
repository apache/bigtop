#!/bin/sh

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

failIfNotOK() {
  if [ $? != 0 ]; then
    exit $?
  fi
}

usage() {
  echo "
usage: $0 <options>
  Required not-so-options:
     --extra-dir=DIR    path to Bigtop distribution files
     --build-dir=DIR    path to Bigtop distribution files
     --server-dir=DIR   path to server package root
     --client-dir=DIR   path to the client package root
     --initd-dir=DIR    path to the server init.d directory

  Optional options:
     --docs-dir=DIR     path to the documentation root
  "
  exit 1
}

OPTS=$(getopt \
  -n $0 \
  -o '' \
  -l 'extra-dir:' \
  -l 'build-dir:' \
  -l 'server-dir:' \
  -l 'client-dir:' \
  -l 'docs-dir:' \
  -l 'initd-dir:' \
  -l 'conf-dir:' \
  -- "$@")

if [ $? != 0 ] ; then
    usage
fi

eval set -- "$OPTS"
while true ; do
    case "$1" in
        --extra-dir)
        EXTRADIR=$2 ; shift 2
        ;;
        --build-dir)
        BUILDDIR=$2 ; shift 2
        ;;
        --server-dir)
        SERVERDIR=$2 ; shift 2
        ;;
        --client-dir)
        CLIENTDIR=$2 ; shift 2
        ;;
        --docs-dir)
        OOZIE_DOCS=$2 ; shift 2
        ;;
        --initd-dir)
        INITDDIR=$2 ; shift 2
        ;;
        --conf-dir)
        CONFDIR=$2 ; shift 2
        ;;
        --)
        shift; break
        ;;
        *)
        echo "Unknown option: $1"
        usage
        ;;
    esac
done

for var in BUILDDIR SERVERDIR CLIENTDIR; do
  if [ -z "$(eval "echo \$$var")" ]; then
    echo Missing param: $var
    usage
  fi
done

if [ ! -d "${BUILDDIR}" ]; then
  echo "Build directory does not exist: ${BUILDDIR}"
  exit 1
fi

if [ -d "${SERVERDIR}" ]; then
  echo "Server directory already exists, delete first: ${SERVERDIR}"
  exit 1
fi

if [ -d "${CLIENTDIR}" ]; then
  echo "Client directory already exists, delete first: ${CLIENTDIR}"
  exit 1
fi

if [ -d "${OOZIE_DOCS}" ]; then
  echo "Docs directory already exists, delete first: ${OOZIE_DOCS}"
  exit 1
fi

OOZIE_BUILD_DIR=${BUILDDIR}

## Install client image first
OOZIE_CLIENT_DIR=${CLIENTDIR}/usr/lib/oozie
OOZIE_MAN_DIR=${CLIENTDIR}/usr/share/man/man1
DOC_DIR=${OOZIE_DOCS:-$PREFIX/usr/share/doc/oozie}
BIN_DIR=${CLIENTDIR}/usr/bin

install -d -m 0755 ${OOZIE_CLIENT_DIR}
failIfNotOK
install -d -m 0755 ${OOZIE_CLIENT_DIR}/bin
failIfNotOK
cp -R ${OOZIE_BUILD_DIR}/bin/oozie ${OOZIE_CLIENT_DIR}/bin
failIfNotOK
cp -R ${OOZIE_BUILD_DIR}/lib ${OOZIE_CLIENT_DIR}
failIfNotOK
install -d -m 0755 ${DOC_DIR}
failIfNotOK
cp -R ${OOZIE_BUILD_DIR}/LICENSE.txt ${DOC_DIR}
failIfNotOK
cp -R ${OOZIE_BUILD_DIR}/NOTICE.txt ${DOC_DIR}
failIfNotOK
cp -R ${OOZIE_BUILD_DIR}/oozie-examples.tar.gz ${DOC_DIR}
failIfNotOK
cp -R ${OOZIE_BUILD_DIR}/readme.txt ${DOC_DIR}
failIfNotOK
cp -R ${OOZIE_BUILD_DIR}/release-log.txt ${DOC_DIR}
failIfNotOK
[ -f ${OOZIE_BUILD_DIR}/PATCH.txt ] && cp ${OOZIE_BUILD_DIR}/PATCH.txt ${DOC_DIR}
# failIfNotOK
cp -R ${OOZIE_BUILD_DIR}/docs/* ${DOC_DIR}
failIfNotOK
install -d -m 0755 ${OOZIE_MAN_DIR}
failIfNotOK
gzip -c ${EXTRADIR}/oozie.1 > ${OOZIE_MAN_DIR}/oozie.1.gz
failIfNotOK

# Create the /usr/bin/oozie wrapper
install -d -m 0755 $BIN_DIR
failIfNotOK
cat > ${BIN_DIR}/oozie <<EOF
#!/bin/sh
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

# Autodetect JAVA_HOME if not defined
if [ -e /usr/libexec/bigtop-detect-javahome ]; then
  . /usr/libexec/bigtop-detect-javahome
elif [ -e /usr/lib/bigtop-utils/bigtop-detect-javahome ]; then
  . /usr/lib/bigtop-utils/bigtop-detect-javahome
fi

exec /usr/lib/oozie/bin/oozie "\$@"
EOF
failIfNotOK
chmod 755 ${BIN_DIR}/oozie
failIfNotOK


## Install server image
OOZIE_SERVER_DIR=${SERVERDIR}/usr/lib/oozie
OOZIE_CONF=${CONFDIR:-"${SERVERDIR}/etc/oozie/conf.dist"}
OOZIE_INITD=${INITDDIR}
OOZIE_DATA=${SERVERDIR}/var/lib/oozie

install -d -m 0755 ${OOZIE_SERVER_DIR}
failIfNotOK
install -d -m 0755 ${OOZIE_SERVER_DIR}/bin
failIfNotOK
install -d -m 0755 ${OOZIE_DATA}
failIfNotOK
cp -R ${OOZIE_BUILD_DIR}/bin/*.sh ${OOZIE_SERVER_DIR}/bin
failIfNotOK
cp -R ${OOZIE_BUILD_DIR}/libtools ${OOZIE_SERVER_DIR}
failIfNotOK

install -d -m 0755 ${OOZIE_CONF}
failIfNotOK
cp ${OOZIE_BUILD_DIR}/conf/* ${OOZIE_CONF}
sed -i -e '/oozie.service.HadoopAccessorService.hadoop.configurations/,/<\/property>/s#<value>\*=hadoop-conf</value>#<value>*=/etc/hadoop/conf</value>#g' \
          ${OOZIE_CONF}/oozie-site.xml
failIfNotOK
cp ${EXTRADIR}/oozie-env.sh ${OOZIE_CONF}
failIfNotOK
install -d -m 0755 ${OOZIE_CONF}/action-conf
failIfNotOK
cp ${EXTRADIR}/hive.xml ${OOZIE_CONF}/action-conf
failIfNotOK
if [ "${OOZIE_INITD}" != "" ]; then
  install -d -m 0755 ${OOZIE_INITD}
  failIfNotOK
  cp -R ${EXTRADIR}/oozie.init ${OOZIE_INITD}/oozie
  failIfNotOK
  chmod 755 ${OOZIE_INITD}/oozie
 failIfNotOK
fi
cp -R ${OOZIE_BUILD_DIR}/oozie-sharelib*.tar.gz ${OOZIE_SERVER_DIR}/oozie-sharelib.tar.gz
failIfNotOK
cp -R ${OOZIE_BUILD_DIR}/oozie-server/webapps ${OOZIE_SERVER_DIR}/webapps
failIfNotOK
ln -s -f /etc/oozie/conf/oozie-env.sh ${OOZIE_SERVER_DIR}/bin
failIfNotOK

# Unpack oozie.war some place reasonable
OOZIE_WEBAPP=${OOZIE_SERVER_DIR}/webapps/oozie
mkdir ${OOZIE_WEBAPP}
failIfNotOK
unzip -d ${OOZIE_WEBAPP} ${OOZIE_BUILD_DIR}/oozie.war
failIfNotOK
mv -f ${OOZIE_WEBAPP}/WEB-INF/lib ${OOZIE_SERVER_DIR}/libserver
failIfNotOK
touch ${OOZIE_SERVER_DIR}/webapps/oozie.war
failIfNotOK

# Create an exploded-war oozie deployment in /var/lib/oozie
install -d -m 0755 ${OOZIE_SERVER_DIR}/oozie-server
failIfNotOK
cp -R ${OOZIE_BUILD_DIR}/oozie-server/conf ${OOZIE_SERVER_DIR}/oozie-server/conf
failIfNotOK
cp ${EXTRADIR}/context.xml ${OOZIE_SERVER_DIR}/oozie-server/conf/
failIfNotOK
cp ${EXTRADIR}/catalina.properties ${OOZIE_SERVER_DIR}/oozie-server/conf/
failIfNotOK
ln -s ../webapps ${OOZIE_SERVER_DIR}/oozie-server/webapps
failIfNotOK
