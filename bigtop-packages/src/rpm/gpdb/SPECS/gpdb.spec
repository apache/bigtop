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
%define man_dir %{_mandir}

%if  %{?suse_version:1}0
%define bin_gpdb /usr/lib/gpdb
%define doc_gpdb %{_docdir}/%{name}
%define autorequire no
%else
%define bin_gpdb /usr/lib/gpdb
%define doc_gpdb %{_docdir}/%{name}-%{gpdb_version}
%define autorequire yes
%endif
%define  debug_package %{nil}

%undefine _auto_set_build_flags

%global __brp_check_rpaths %{nil}

Name: gpdb
Version: %{gpdb_version}
Release: %{gpdb_release}
Summary: Greenplum MPP database enginer
URL: https://github.com/greenplum-db/gpdb
Group: Development/Libraries
Buildroot: %{_topdir}/INSTALL/%{name}-%{version}
License: ASL 2.0
Source0: gpdb-%{gpdb_base_version}.tar.gz
Source1: do-component-build
Source2: install_gpdb.sh
Source3: do-component-configure
#BIGTOP_PATCH_FILES
AutoReqProv: %{autorequire}
%if 0%{?openEuler}
Requires: bigtop-utils >= 0.7, gcc, libffi-devel, make, openssl-devel, python3-devel
%else
Requires: bigtop-utils >= 0.7, gcc, libffi-devel, make, openssl-devel, python2-devel
%endif

%description
gpdb

%prep
%if 0%{?openEuler}
sed -i "s/python2/python3/g" $RPM_SOURCE_DIR/patch1-specify-python-version.diff 
sed -i "s/python3.7/python2.7/g" $RPM_SOURCE_DIR/patch1-specify-python-version.diff
%endif

%setup -n %{name}-%{gpdb_base_version}

#BIGTOP_PATCH_COMMANDS

%build
%if 0%{?openEuler}
rm gpMgmt/bin/pythonSrc/PyGreSQL-4.0 -rf
cd gpMgmt/bin/pythonSrc/
wget https://github.com/PyGreSQL/PyGreSQL/archive/refs/tags/5.1.2.tar.gz  --no-check-certificate
tar -xf 5.1.2.tar.gz
rm -rf 5.1.2.tar.gz
cd ../../../
pip install 2to3
python3 %{_bindir}/2to3 -w .
for i in `find . -name "*.bak"`;do rm -rf $i ;done
sed -i 's|PYGRESQL_VERSION=4.0|PYGRESQL_VERSION=5.1.2|g' gpMgmt/bin/Makefile
sed -i '191 c PYTHON_VERSION=$(shell python3 -c "import sys; print ('%s.%s' % (sys.version_info[0:2])"))' gpMgmt/bin/Makefile
sed -i "91 c inc_dirs = [os.path.join(pkginc.decode(encoding='UTF-8'), 'server'), os.path.join(pkginc.decode(encoding='UTF-8'), 'internal')]" src/test/regress/checkinc.py
sed -i 's|if os.path.exists(os.path.join(inc,i)):|if os.path.exists(os.path.join(inc.decode(encoding="UTF-8"),i)):|g' src/test/regress/checkinc.py
%endif
bash %{SOURCE3} %{bin_gpdb}
bash %{SOURCE1}

%install
%__rm -rf $RPM_BUILD_ROOT
bash %{SOURCE2} %{_tmppath}
mkdir -p $RPM_BUILD_ROOT%{bin_gpdb}
cp -f -r %{_tmppath}%{bin_gpdb}/* $RPM_BUILD_ROOT/%{bin_gpdb}
%__rm -rf %{_tmppath}%{bin_gpdb}

%files
%defattr(-,root,root)
%{bin_gpdb}

%changelog
