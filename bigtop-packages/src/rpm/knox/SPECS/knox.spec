%define __jar_repack 0

Name:		knox
Version:	%{knox_version}
Release:	%{knox_release}
Summary:	knox

Group:		Application/Server
License:	Apache License, Version 2.0
URL:		  http://knox.incubator.apache.org/
Source0:	knox-%{knox_version}.tar.gz
Source1:  do-component-build
Source2:  install_knox.sh

BuildArch:  noarch
Requires:	bash
Provides: 	knox
AutoReqProv: 	no

%description
knox is a scalable and extensible set of core foundational governance services ? enabling enterprises to effectively and efficiently meet their compliance requirements within Hadoop and allows integration with the whole enterprise data ecosystem.

%package shell
Summary: Knox shell
Group: System/Daemons
Requires: %{name} = %{version}-%{release}
Requires(pre): %{name} = %{version}-%{release}

%description shell
This knox shell package


%prep
%setup -q -n knox-%{knox_version}

%build
bash %{SOURCE1}

%install
%__rm -rf $RPM_BUILD_ROOT

/bin/bash %{SOURCE2} $RPM_BUILD_ROOT %{knox_version}

%files
%config /etc/knox
%doc
/usr/lib/knox


%files shell
%config /etc/knox-shell
%doc
/usr/lib/knox-shell



%changelog
