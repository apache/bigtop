%define __jar_repack 0

Name:		nifi-registry
Version:	%{nifi_registry_version}
Release:	%{nifi_registry_release}
Summary:	nifi-registry

Group:		Application/Server
License:	Apache License, Version 2.0
URL:		  http://nifi-registry.incubator.apache.org/
Source0:	nifi-registry-%{nifi_registry_version}.tar.gz
Source1:  do-component-build
Source2:  install_nifi-registry.sh
#BIGTOP_PATCH_FILES

BuildArch:  noarch
Requires:	bash
Provides: 	nifi-registry
AutoReqProv: 	no

%description
Apache nifi-registry  is a software project designed to automate the flow of data between software systems.

%prep
%setup -q -n nifi-registry-%{nifi_registry_version}
#BIGTOP_PATCH_COMMANDS

%build
bash %{SOURCE1}

%install
%__rm -rf $RPM_BUILD_ROOT

/bin/bash %{SOURCE2} $RPM_BUILD_ROOT %{nifi_registry_version}

%files
%config /etc/nifi-registry
%doc
/usr/lib/nifi-registry

%changelog
