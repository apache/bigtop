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

%define ambari_name ambari 
%define _binaries_in_noarch_packages_terminate_build   0
%define _unpackaged_files_terminate_build 0

%if  %{?suse_version:1}0
%define doc_ambari %{_docdir}/ambari-doc
%global initd_dir %{_sysconfdir}/rc.d
%else
%define doc_ambari %{_docdir}/ambari-doc-%{ambari_version}
%global initd_dir %{_sysconfdir}/rc.d/init.d
%endif

# disable repacking jars
%define __os_install_post %{nil}

Name: ambari
Version: %{ambari_version}
Release: %{ambari_release}
Summary: Ambari
URL: http://ambari.apache.org
Group: Development
BuildArch: noarch
Buildroot: %(mktemp -ud %{_tmppath}/apache-%{ambari_name}-%{version}-%{release}-XXXXXX)
License: ASL 2.0 
Source0: apache-%{ambari_name}-%{ambari_base_version}-src.tar.gz
Source1: do-component-build 
Source2: install_%{ambari_name}.sh
Source3: bigtop.bom
#BIGTOP_PATCH_FILES
# FIXME
AutoProv: no
AutoReqProv: no

%description
Ambari

%prep
%setup -n apache-%{ambari_name}-%{ambari_base_version}-src

#BIGTOP_PATCH_COMMANDS

%build
bash $RPM_SOURCE_DIR/do-component-build

%install
%__rm -rf $RPM_BUILD_ROOT
AMBARI_VERSION=%{ambari_version} bash $RPM_SOURCE_DIR/install_ambari.sh \
          --build-dir=`pwd` \
          --distro-dir=$RPM_SOURCE_DIR \
          --source-dir=`pwd` \
          --prefix=$RPM_BUILD_ROOT
%__install -d -m 0755 $RPM_BUILD_ROOT/%{initd_dir}
%__mv ${RPM_BUILD_ROOT}/etc/init.d/ambari-server ${RPM_BUILD_ROOT}/%{initd_dir} || :
 
%package server
Summary: Ambari Server
Group: Development/Libraries
# BIGTOP-3139: install initscripts to workaround service command not available issue
Requires: openssl, postgresql-server >= 8.1, python2 >= 2.6, curl, initscripts
AutoProv: no
AutoReqProv: no
%description server
Ambari Server

%pre server
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
# limitations under the License

STACKS_FOLDER="/var/lib/ambari-server/resources/stacks"
STACKS_FOLDER_OLD=/var/lib/ambari-server/resources/stacks_$(date '+%d_%m_%y_%H_%M').old

COMMON_SERVICES_FOLDER="/var/lib/ambari-server/resources/common-services"
COMMON_SERVICES_FOLDER_OLD=/var/lib/ambari-server/resources/common-services_$(date '+%d_%m_%y_%H_%M').old

AMBARI_VIEWS_FOLDER="/var/lib/ambari-server/resources/views"
AMBARI_VIEWS_BACKUP_FOLDER="$AMBARI_VIEWS_FOLDER/backups"

if [ -d "/etc/ambari-server/conf.save" ]
then
    mv /etc/ambari-server/conf.save /etc/ambari-server/conf_$(date '+%d_%m_%y_%H_%M').save
fi

if [ -d "$STACKS_FOLDER" ]
then
    mv -f "$STACKS_FOLDER" "$STACKS_FOLDER_OLD"
fi

if [ -d "$COMMON_SERVICES_FOLDER_OLD" ]
then
    mv -f "$COMMON_SERVICES_FOLDER" "$COMMON_SERVICES_FOLDER_OLD"
fi

if [ ! -d "$AMBARI_VIEWS_BACKUP_FOLDER" ] && [ -d "$AMBARI_VIEWS_FOLDER" ]
then
    mkdir "$AMBARI_VIEWS_BACKUP_FOLDER"
fi

if [ -d "$AMBARI_VIEWS_FOLDER" ] && [ -d "$AMBARI_VIEWS_BACKUP_FOLDER" ]
then
    cp -u $AMBARI_VIEWS_FOLDER/*.jar $AMBARI_VIEWS_BACKUP_FOLDER/
fi

exit 0

%post server
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
# limitations under the License

if [ -e "/usr/sbin/ambari-server" ]; then # Check is needed for upgrade
    # Remove link created by previous package version
    rm -f /usr/sbin/ambari-server
fi

ln -s /etc/init.d/ambari-server /usr/sbin/ambari-server

case "$1" in
  1) # Action install
    if [ -f "/var/lib/ambari-server/install-helper.sh" ]; then
        /var/lib/ambari-server/install-helper.sh install
    fi
    chkconfig --add ambari-server
  ;;
  2) # Action upgrade
    if [ -f "/var/lib/ambari-server/install-helper.sh" ]; then
        /var/lib/ambari-server/install-helper.sh upgrade
    fi
  ;;
esac

exit 0

%preun server
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
# limitations under the License

# WARNING: This script is performed not only on uninstall, but also
# during package update. See http://www.ibm.com/developerworks/library/l-rpm2/
# for details

if [ "$1" -eq 0 ]; then  # Action is uninstall
    /usr/sbin/ambari-server stop > /dev/null 2>&1
    if [ -d "/etc/ambari-server/conf.save" ]; then
        mv /etc/ambari-server/conf.save /etc/ambari-server/conf_$(date '+%d_%m_%y_%H_%M').save
    fi

    if [ -e "/usr/sbin/ambari-server" ]; then
        # Remove link created during install
        rm /usr/sbin/ambari-server
    fi

    mv /etc/ambari-server/conf /etc/ambari-server/conf.save

    if [ -f "/var/lib/ambari-server/install-helper.sh" ]; then
      /var/lib/ambari-server/install-helper.sh remove
    fi

    chkconfig --list | grep ambari-server && chkconfig --del ambari-server
fi

exit 0

%posttrans server
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
# limitations under the License

PYPATH=`find /usr/lib -maxdepth 1 -name 'python*'`
PYLIB_DIR=`echo ${PYPATH} | awk '{print $1}'`
RESOURCE_MANAGEMENT_DIR= "${PYLIB_DIR}/site-packages/resource_management"
RESOURCE_MANAGEMENT_DIR_SERVER="/usr/lib/ambari-server/lib/resource_management"
JINJA_DIR="${PYLIB_DIR}/site-packages/ambari_jinja2"
JINJA_SERVER_DIR="/usr/lib/ambari-server/lib/ambari_jinja2"
AMBARI_SERVER_EXECUTABLE_LINK="/usr/sbin/ambari-server"
AMBARI_SERVER_EXECUTABLE="/etc/init.d/ambari-server"


# needed for upgrade though ambari-2.2.2
rm -f "$AMBARI_SERVER_EXECUTABLE_LINK"
ln -s "$AMBARI_SERVER_EXECUTABLE" "$AMBARI_SERVER_EXECUTABLE_LINK"

# remove RESOURCE_MANAGEMENT_DIR if it's a directory
if [ -d "$RESOURCE_MANAGEMENT_DIR" ]; then  # resource_management dir exists
  if [ ! -L "$RESOURCE_MANAGEMENT_DIR" ]; then # resource_management dir is not link
    rm -rf "$RESOURCE_MANAGEMENT_DIR"
  fi
fi
# setting resource_management shared resource
if [ ! -d "$RESOURCE_MANAGEMENT_DIR" ]; then
  ln -s ${RESOURCE_MANAGEMENT_DIR_SERVER} ${RESOURCE_MANAGEMENT_DIR}
fi

# setting jinja2 shared resource
if [ ! -d "$JINJA_DIR" ]; then
  ln -s ${JINJA_SERVER_DIR} ${JINJA_DIR}
fi

exit 0

%package agent
Summary: Ambari Agent
Group: Development/Libraries
Requires: openssl, zlib, python2 >= 2.6, initscripts
AutoProv: no
AutoReqProv: no
%description agent
Ambari Agent

%pre agent
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
# limitations under the License

STACKS_FOLDER="/var/lib/ambari-agent/cache/stacks"
STACKS_FOLDER_OLD=/var/lib/ambari-agent/cache/stacks_$(date '+%d_%m_%y_%H_%M').old

COMMON_SERVICES_FOLDER="/var/lib/ambari-agent/cache/common-services"
COMMON_SERVICES_FOLDER_OLD=/var/lib/ambari-agent/cache/common-services_$(date '+%d_%m_%y_%H_%M').old

if [ -d "/etc/ambari-agent/conf.save" ]
then
    mv /etc/ambari-agent/conf.save /etc/ambari-agent/conf_$(date '+%d_%m_%y_%H_%M').save
fi

BAK=/etc/ambari-agent/conf/ambari-agent.ini.old
ORIG=/etc/ambari-agent/conf/ambari-agent.ini

BAK_SUDOERS=/etc/sudoers.d/ambari-agent.bak
ORIG_SUDOERS=/etc/sudoers.d/ambari-agent

[ -f $ORIG ] && mv -f $ORIG $BAK
[ -f $ORIG_SUDOERS ] && echo "Moving $ORIG_SUDOERS to $BAK_SUDOERS. Please restore the file if you were using it for ambari-agent non-root functionality" && mv -f $ORIG_SUDOERS $BAK_SUDOERS

if [ -d "$STACKS_FOLDER" ]
then
    mv -f "$STACKS_FOLDER" "$STACKS_FOLDER_OLD"
fi

if [ -d "$COMMON_SERVICES_FOLDER_OLD" ]
then
    mv -f "$COMMON_SERVICES_FOLDER" "$COMMON_SERVICES_FOLDER_OLD"
fi

exit 0

%post agent
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
# limitations under the License


case "$1" in
  1) # Action install
    if [ -f "/var/lib/ambari-agent/install-helper.sh" ]; then
        /var/lib/ambari-agent/install-helper.sh install
    fi
  chkconfig --add ambari-agent
  ;;
  2) # Action upgrade
    if [ -d "/etc/ambari-agent/conf.save" ]; then
        cp -f /etc/ambari-agent/conf.save/* /etc/ambari-agent/conf
        mv /etc/ambari-agent/conf.save /etc/ambari-agent/conf_$(date '+%d_%m_%y_%H_%M').save
    fi

    if [ -f "/var/lib/ambari-agent/install-helper.sh" ]; then
        /var/lib/ambari-agent/install-helper.sh upgrade
    fi
  ;;
esac


BAK=/etc/ambari-agent/conf/ambari-agent.ini.old
ORIG=/etc/ambari-agent/conf/ambari-agent.ini

if [ -f $BAK ]; then
  if [ -f "/var/lib/ambari-agent/upgrade_agent_configs.py" ]; then
    /var/lib/ambari-agent/upgrade_agent_configs.py
  fi
  mv $BAK ${BAK}_$(date '+%d_%m_%y_%H_%M').save
fi


exit 0

%preun agent
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
# limitations under the License

# WARNING: This script is performed not only on uninstall, but also
# during package update. See http://www.ibm.com/developerworks/library/l-rpm2/
# for details


if [ "$1" -eq 0 ]; then  # Action is uninstall
    /var/lib/ambari-agent/bin/ambari-agent stop > /dev/null 2>&1
    if [ -d "/etc/ambari-agent/conf.save" ]; then
        mv /etc/ambari-agent/conf.save /etc/ambari-agent/conf_$(date '+%d_%m_%y_%H_%M').save
    fi
    mv /etc/ambari-agent/conf /etc/ambari-agent/conf.save

    if [ -f "/var/lib/ambari-agent/install-helper.sh" ]; then
      /var/lib/ambari-agent/install-helper.sh remove
    fi

    chkconfig --list | grep ambari-server && chkconfig --del ambari-server
fi

exit 0

%posttrans agent
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
# limitations under the License

PYPATH=`find /usr/lib -maxdepth 1 -name 'python*'`
PYLIB_DIR=`echo $PYPATH | awk '{print $1}'`
RESOURCE_MANAGEMENT_DIR="$PYLIB_DIR/site-packages/resource_management"
RESOURCE_MANAGEMENT_DIR_AGENT="/usr/lib/ambari-agent/lib/resource_management"
JINJA_DIR="$PYLIB_DIR/site-packages/ambari_jinja2"
JINJA_AGENT_DIR="/usr/lib/ambari-agent/lib/ambari_jinja2"

# remove RESOURCE_MANAGEMENT_DIR if it's a directory
if [ -d "$RESOURCE_MANAGEMENT_DIR" ]; then  # resource_management dir exists
  if [ ! -L "$RESOURCE_MANAGEMENT_DIR" ]; then # resource_management dir is not link
    rm -rf "$RESOURCE_MANAGEMENT_DIR"
  fi
fi
# setting resource_management shared resource
if [ ! -d "$RESOURCE_MANAGEMENT_DIR" ]; then
  ln -s "$RESOURCE_MANAGEMENT_DIR_AGENT" "$RESOURCE_MANAGEMENT_DIR"
fi

# setting jinja2 shared resource
if [ ! -d "$JINJA_DIR" ]; then
  ln -s "$JINJA_AGENT_DIR" "$JINJA_DIR"
fi

exit 0

%files server
%attr(644,root,root) /etc/init/ambari-server.conf
%defattr(644,root,root,755)
/usr/lib/ambari-server
%attr(755,root,root) /usr/sbin/ambari-server.py
%attr(755,root,root) /usr/sbin/ambari_server_main.py
%attr(755,root,root) %{initd_dir}/ambari-server
/var/lib/ambari-server
%attr(755,root,root) /var/lib/ambari-server/ambari-python-wrap
%config  /etc/ambari-server/conf
%config %attr(700,root,root) /var/lib/ambari-server//ambari-env.sh
%attr(700,root,root) /var/lib/ambari-server//ambari-sudo.sh
%attr(700,root,root) /var/lib/ambari-server//install-helper.sh
%attr(700,root,root) /var/lib/ambari-server/keys/db
%attr(755,root,root) /var/lib/ambari-server/resources/stacks/stack_advisor.py
%dir %attr(755,root,root) /var/lib/ambari-server/data/tmp
%dir %attr(700,root,root) /var/lib/ambari-server/data/cache
%attr(755,root,root) /var/lib/ambari-server/resources/scripts
%attr(755,root,root) /var/lib/ambari-server/resources/views
%attr(755,root,root) /var/lib/ambari-server/resources/custom_actions
%attr(755,root,root) /var/lib/ambari-server/resources/host_scripts
%dir  /var/lib/ambari-server/resources/upgrade
%dir  /var/run/ambari-server
%dir  /var/run/ambari-server/bootstrap
%dir  /var/run/ambari-server/stack-recommendations
%dir  /var/log/ambari-server

%files agent
/usr/lib/ambari-agent
%attr(644,root,root) /etc/init/ambari-agent.conf
%attr(755,root,root) /var/lib/ambari-agent/ambari-python-wrap
%attr(755,root,root) /var/lib/ambari-agent/ambari-sudo.sh
%attr(-,root,root) /usr/lib/ambari-agent/lib/ambari_commons
%attr(-,root,root) /usr/lib/ambari-agent/lib/resource_management
%attr(755,root,root) /usr/lib/ambari-agent/lib/ambari_jinja2
%attr(755,root,root) /usr/lib/ambari-agent/lib/ambari_simplejson
%attr(755,root,root) /usr/lib/ambari-agent/lib/examples
%attr(755,root,root) /etc/ambari-agent/conf/ambari-agent.ini
%attr(755,root,root) /etc/ambari-agent/conf/logging.conf.sample
%attr(755,root,root) /var/lib/ambari-agent/bin/ambari-agent 
%config %attr(700,root,root) /var/lib/ambari-agent/ambari-env.sh
%attr(700,root,root) /var/lib/ambari-agent/install-helper.sh
%attr(700,root,root) /var/lib/ambari-agent/upgrade_agent_configs.py
%dir %attr(755,root,root) /var/run/ambari-agent
%dir %attr(755,root,root) /var/lib/ambari-agent/data
%dir %attr(777,root,root) /var/lib/ambari-agent/tmp
%dir %attr(755,root,root) /var/lib/ambari-agent/keys
%dir %attr(755,root,root) /var/log/ambari-agent
%attr(755,root,root) /etc/init.d/ambari-agent
%attr(755,root,root) /var/lib/ambari-agent/data
%attr(755,root,root) /var/lib/ambari-agent/cache
%attr(755,root,root) /var/lib/ambari-agent/cred
%attr(755,root,root) /var/lib/ambari-agent/tools
