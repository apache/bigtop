%define __jar_repack 0

Name:		nifi
Version:	%{nifi_version}
Release:	%{nifi_release}
Summary:	nifi

Group:		Application/Server
License:	Apache License, Version 2.0
URL:		  http://nifi.incubator.apache.org/
Source0:	nifi-%{nifi_version}.tar.gz
Source1:  do-component-build
Source2:  install_nifi.sh
#BIGTOP_PATCH_FILES

BuildArch:  noarch
Requires:	bash
Provides: 	nifi
AutoReqProv: 	no

%description
Apache NiFi  is a software project designed to automate the flow of data between software systems.

%prep
%setup -q -n nifi-%{nifi_version}
#BIGTOP_PATCH_COMMANDS

%build
bash %{SOURCE1}

%install
%__rm -rf $RPM_BUILD_ROOT

/bin/bash %{SOURCE2} $RPM_BUILD_ROOT %{nifi_version}

%files
%config /etc/nifi
%doc
/usr/lib/nifi-server

%changelog
