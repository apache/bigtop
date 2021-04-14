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

usage() {
  [ $# -eq 0 ] || echo "$@"
  echo "
usage: $0 [options] command [command-arg] [command [command-arg]] ...

Options:
    --solr solr_uri
    --zk   zk_ensemble
    --help
    --quiet

Commands:
    init        [--force]

    instancedir [--generate path]
                [--create name path]
                [--update name path]
                [--get name path]
                [--delete name]
                [--list]

    collection  [--create name -s <numShards>
                              [-c <collection.configName>]
                              [-r <replicationFactor>]
                              [-m <maxShardsPerNode>]
                              [-n <createNodeSet>]]
                [--delete name]
                [--reload name]
                [--stat name]
                [--deletedocs name]
                [--list]
                [--config name <json>]

    core        [--create name [-p name=value]...]
                [--reload name]
                [--unload name]
                [--status name]
  "
  exit 1
}

die() {
  echo "$1"
  exit 1
}

# FIXME: this is here only for testing purposes
local_coreconfig() {
  case "$2" in
    put)
      echo "$4" > "/var/lib/solr/$3"
      ;;
    list)
      ls -d /var/lib/solr/*/conf 2>/dev/null | sed -e 's#var/lib/solr#configs#'
      ;;
    clear)
      if [ "$3" != "/" ] ; then
        sudo -u solr rm -rf /var/lib/solr/`basename $3`/* 2>/dev/null
      fi
      ;;
    upconfig)
      # the following trick gets us around permission issues
      rm -rf /tmp/solr_conf.$$
      cp -r $4 /tmp/solr_conf.$$
      chmod o+r -R /tmp/solr_conf.$$
      sudo -u solr bash -c "mkdir /var/lib/solr/$6 2>/dev/null ; cp -r /tmp/solr_conf.$$ /var/lib/solr/$6/conf"
      RES=$?
      rm -rf /tmp/solr_conf.$$
      return $RES
      ;;
    downconfig)
      mkdir -p $4
      cp -r /var/lib/solr/$6/conf/* $4
      ;;
  esac
}

solr_webapi() {
  # If SOLR_ADMIN_URI wasn't given explicitly via --solr we need to guess it
  if [ -z "$SOLR_ADMIN_URI" ] ; then
    for node in `get_solr_state | sed -ne 's#/live_nodes/\(.*:[0-9][0-9]*\).*$#\1#p'` localhost:$SOLR_PORT ; do
      if $SOLR_ADMIN_CURL "http://$node/solr" >/dev/null 2>&1 ; then
        SOLR_ADMIN_URI="http://$node/solr"
        break
      fi
    done
    [ -n "$SOLR_ADMIN_URI" ] || die "Error: can't discover Solr URI. Please specify it explicitly via --solr."
  fi
  URI="$SOLR_ADMIN_URI$1"
  shift
  local WEB_OUT=$($SOLR_ADMIN_CURL $URI "$@" | sed -e 's#>#>\n#g')
  if [ $? -eq 0 ] && (echo "$WEB_OUT" | grep -q 'HTTP/.*200.*OK') ; then
    echo "$WEB_OUT" | egrep -q '<lst name="(failure|exception|error)">' || return 0
  fi

  die "Error: A call to SolrCloud WEB APIs failed: $WEB_OUT"
}

get_solr_state() {
  if [ -z "$SOLR_STATE" ] ; then
    SOLR_STATE=`eval $SOLR_ADMIN_ZK_CMD -cmd list 2>/dev/null`
  fi

  echo "$SOLR_STATE" | grep -v '^/ '
}

SOLR_CONF_DIR=${SOLR_CONF_DIR:-/etc/solr/conf}

if [ -e "$SOLR_CONF_DIR/solr-env.sh" ] ; then
  . "$SOLR_CONF_DIR/solr-env.sh"
elif [ -e /etc/default/solr ] ; then
  . /etc/default/solr
fi

SOLR_PORT=${SOLR_PORT:-8983}
SOLR_ADMIN_CURL='curl -i --retry 5 -s -L -k --negotiate -u :'
SOLR_ADMIN_CHAT=echo
SOLR_ADMIN_API_CMD='solr_webapi'

SOLR_INSTALL_DIR=/usr/lib/solr

# Autodetect JAVA_HOME if not defined
. /usr/lib/bigtop-utils/bigtop-detect-javahome

# First eat up all the global options

while test $# != 0 ; do
  case "$1" in
    --help)
      usage
      ;;
    --quiet)
      SOLR_ADMIN_CHAT=/bin/true
      shift 1
      ;;
    --solr)
      [ $# -gt 1 ] || usage "Error: $1 requires an argument"
      SOLR_ADMIN_URI="$2"
      shift 2
      ;;
    --zk)
      [ $# -gt 1 ] || usage "Error: $1 requires an argument"
      SOLR_ZK_ENSEMBLE="$2"
      shift 2
      ;;
    *)
      break
      ;;
  esac
done

if [ -z "$SOLR_ZK_ENSEMBLE" ] ; then
  SOLR_ADMIN_ZK_CMD="local_coreconfig"
  cat >&2 <<-__EOT__
	Warning: Non-SolrCloud mode has been completely deprecated
	Please configure SolrCloud via SOLR_ZK_ENSEMBLE setting in
	/etc/default/solr
	If you running remotely, please use --zk zk_ensemble.
	__EOT__
else
  SOLR_ADMIN_ZK_CMD='${JAVA_HOME}/bin/java -classpath "${SOLR_INSTALL_DIR}/server/solr-webapp/webapp/WEB-INF/lib/*:${SOLR_INSTALL_DIR}/server/lib/ext/*:${SOLR_INSTALL_DIR}/server/lib/*" org.apache.solr.cloud.ZkCLI -zkhost $SOLR_ZK_ENSEMBLE 2>/dev/null'
fi


# Now start parsing commands -- there has to be at least one!
[ $# -gt 0 ] || usage
while test $# != 0 ; do
  case "$1" in
    debug-dump)
      get_solr_state

      shift 1
      ;;

    init)
      if [ "$2" == "--force" ] ; then
        shift 1
      else
        LIVE_NODES=`get_solr_state | sed -ne 's#/live_nodes/##p'`

        if [ -n "$LIVE_NODES" ] ; then
          die "Warning: It appears you have live SolrCloud nodes running: `printf '\n%s\nPlease shut them down.' \"${LIVE_NODES}\"`"
        elif [ -n "`get_solr_state`" ] ; then
          die "Warning: Solr appears to be initialized (use --force to override)"
        fi
      fi

      eval $SOLR_ADMIN_ZK_CMD -cmd makepath / > /dev/null 2>&1 || :
      eval $SOLR_ADMIN_ZK_CMD -cmd clear /    || die "Error: failed to initialize Solr"

      eval $SOLR_ADMIN_ZK_CMD -cmd put /solr.xml "'$(cat $SOLR_INSTALL_DIR/server/solr/solr.xml)'"

      shift 1
      ;;

    coreconfig)
      $SOLR_ADMIN_CHAT  "Warning: coreconfig is deprecated, please use instancedir instead (consult documentation on differences in behaviour)."
      shift 1
      set instancedir "$@"
      ;;
    instancedir)
      [ $# -gt 1 ] || usage "Error: incorrect specification of arguments for $1"
      case "$2" in
        --create)
            [ $# -gt 3 ] || usage "Error: incorrect specification of arguments for $1 $2"

            if [ -d $4/conf ] ; then
              INSTANCE_DIR="$4/conf"
            else
              INSTANCE_DIR="$4"
            fi

            [ -e ${INSTANCE_DIR}/solrconfig.xml -a -e ${INSTANCE_DIR}/managed-schema ] || die "Error: ${INSTANCE_DIR} must be a directory with at least solrconfig.xml and managed-schema"

            get_solr_state | grep -q '^ */configs/'"$3/" && die "Error: \"$3\" configuration already exists. Aborting. Try --update if you want to override"

            $SOLR_ADMIN_CHAT "Uploading configs from ${INSTANCE_DIR} to $SOLR_ZK_ENSEMBLE. This may take up to a minute."
            eval $SOLR_ADMIN_ZK_CMD -cmd upconfig -confdir ${INSTANCE_DIR} -confname $3 2>/dev/null || die "Error: can't upload configuration"
            shift 4
            ;;
        --update)
            [ $# -gt 3 ] || usage "Error: incorrect specification of arguments for $1 $2"

            if [ -d $4/conf ] ; then
              INSTANCE_DIR="$4/conf"
            else
              INSTANCE_DIR="$4"
            fi

            [ -e ${INSTANCE_DIR}/solrconfig.xml -a -e ${INSTANCE_DIR}/managed-schema ] || die "Error: ${INSTANCE_DIR} must be a directory with at least solrconfig.xml and managed-schema"

            eval $SOLR_ADMIN_ZK_CMD -cmd clear /configs/$3 2>/dev/null || die "Error: can't delete configuration"

            $SOLR_ADMIN_CHAT "Uploading configs from ${INSTANCE_DIR} to $SOLR_ZK_ENSEMBLE. This may take up to a minute."
            eval $SOLR_ADMIN_ZK_CMD -cmd upconfig -confdir ${INSTANCE_DIR} -confname $3 2>/dev/null || die "Error: can't upload configuration"
            shift 4
            ;;
        --get)
            [ $# -gt 3 ] || usage "Error: incorrect specification of arguments for $1 $2"

            [ -e "$4" ] && die "Error: subdirectory $4 already exists"

            $SOLR_ADMIN_CHAT "Downloading configs from $SOLR_ZK_ENSEMBLE to $4. This may take up to a minute."
            eval $SOLR_ADMIN_ZK_CMD -cmd downconfig -confdir "$4/conf" -confname "$3" 2>/dev/null || die "Error: can't download configuration"
            shift 4
            ;;
        --delete)
            [ $# -gt 2 ] || usage "Error: incorrect specification of arguments for $1"

            eval $SOLR_ADMIN_ZK_CMD -cmd clear /configs/$3 2>/dev/null || die "Error: can't delete configuration"
            shift 3
            ;;
        --generate)
            [ $# -gt 2 ] || usage "Error: incorrect specification of arguments for $1"

            [ -e "$3" ] && die "Error: subdirectory $3 already exists"

            mkdir -p "$3" > /dev/null 2>&1
            [ -d "$3" ] || usage "Error: $3 has to be a directory"
            # sample_techproducts_configs is used as default, this provides many common used optional features
            cp -r ${SOLR_INSTALL_DIR}/server/solr/configsets/sample_techproducts_configs/conf "$3/conf"
            shift 3
            ;;
        --list)
            get_solr_state | sed -n -e '/\/configs\//s#^.*/configs/\([^/]*\)/.*$#\1#p' | sort -u
            shift 2
            ;;
        *)
            shift 1
            ;;
      esac
      ;;

    collection)
      [ "$2" = "--list" ] || [ $# -gt 2 ] || usage "Error: incorrect specification of arguments for $1"
      case "$2" in
        --create)
            COL_CREATE_NAME=$3
            COL_CREATE_NUMSHARDS=1
            shift 3
            while test $# -gt 0 ; do
              case "$1" in
                -s)
                  [ $# -gt 1 ] || usage "Error: collection --create name $1 requires an argument"
                  COL_CREATE_NUMSHARDS="$2"
                  shift 2
                  ;;
                -c)
                  [ $# -gt 1 ] || usage "Error: collection --create name $1 requires an argument"
                  COL_CREATE_CONFNAME="$2"
                  shift 2
                  ;;
                -r)
                  [ $# -gt 1 ] || usage "Error: collection --create name $1 requires an argument"
                  COL_CREATE_REPL="$2"
                  shift 2
                  ;;
                -m)
                  [ $# -gt 1 ] || usage "Error: collection --create name $1 requires an argument"
                  COL_CREATE_MAXSHARDS="$2"
                  shift 2
                  ;;
                -n)
                  [ $# -gt 1 ] || usage "Error: collection --create name $1 requires an argument"
                  COL_CREATE_NODESET="$2"
                  shift 2
                  ;;
                 *)
                  break
                  ;;
              esac
            done

            COL_CREATE_CALL="&name=${COL_CREATE_NAME}"
            if [ "$COL_CREATE_NUMSHARDS" -gt 0 ] ; then
              COL_CREATE_CALL="${COL_CREATE_CALL}&numShards=${COL_CREATE_NUMSHARDS}"
            else
              usage "Error: collection --create name needs to have more than 0 shards specified"
            fi
            [ -n "$COL_CREATE_CONFNAME" ] && COL_CREATE_CALL="${COL_CREATE_CALL}&collection.configName=${COL_CREATE_CONFNAME}"
            [ -n "$COL_CREATE_REPL" ] && COL_CREATE_CALL="${COL_CREATE_CALL}&replicationFactor=${COL_CREATE_REPL}"
            [ -n "$COL_CREATE_MAXSHARDS" ] && COL_CREATE_CALL="${COL_CREATE_CALL}&maxShardsPerNode=${COL_CREATE_MAXSHARDS}"
            [ -n "$COL_CREATE_NODESET" ] && COL_CREATE_CALL="${COL_CREATE_CALL}&createNodeSet=${COL_CREATE_NODESET}"

            eval $SOLR_ADMIN_API_CMD "'/admin/collections?action=CREATE${COL_CREATE_CALL}'"

            shift 4
            ;;
        --config)
            eval $SOLR_ADMIN_API_CMD "'/$3/config'" -H "'Content-Type: application/json'" "--data-binary '$4'"
            shift 4
            ;;
        --delete|--reload)
            COL_ACTION=`echo $2 | tr '[a-z]-' '[A-Z] '`
            eval $SOLR_ADMIN_API_CMD "'/admin/collections?action=`echo $COL_ACTION`&name=$3'"
            shift 3
            ;;
        --deletedocs)
            eval $SOLR_ADMIN_API_CMD "'/$3/update?commit=true'" -H "'Content-Type: text/xml'" "--data-binary '<delete><query>*:*</query></delete>'"
            shift 3
            ;;
        --stat)
            get_solr_state | sed -ne '/\/collections\//s#^.*/collections/##p' | sed -ne '/election\//s###p' | grep "$3/"
            shift 3
            ;;
        --list)
            get_solr_state | sed -ne '/\/collections\/[^\/]*$/s#^.*/collections/##p'
            shift 2
            ;;
        *)
            shift 1
            ;;
      esac
      ;;

    core)
      [ $# -gt 2 ] || usage "Error: incorrect specification of arguments for $1"
      case "$2" in
        --create)
          CORE_CREATE_NAME="$3"
          shift 3
          while test $# -gt 0 ; do
            case "$1" in
              -p)
                [ $# -gt 1 ] || usage "Error: core --create name $1 requires an argument of key=value"
                CORE_KV_PAIRS="${CORE_KV_PAIRS}&${2}"
                shift 2
                ;;
               *)
                break
                ;;
            esac
          done
          [ -n "$CORE_KV_PAIRS" ] || CORE_KV_PAIRS="&instanceDir=${CORE_CREATE_NAME}"

          eval $SOLR_ADMIN_API_CMD "'/admin/cores?action=CREATE&name=${CORE_CREATE_NAME}${CORE_KV_PAIRS}'"
          ;;
        --reload|--unload|--status)
          CORE_ACTION=`echo $2 | tr '[a-z]-' '[A-Z] '`
          eval $SOLR_ADMIN_API_CMD "'/admin/cores?action=`echo $CORE_ACTION`&core=$3'"
          shift 3
          ;;
        *)
          shift 1
          ;;
      esac
      ;;

    *)
      usage "Error: unrecognized command $1"
      ;;
  esac
done

# If none of the above commands ended up calling die -- we're OK
exit 0
