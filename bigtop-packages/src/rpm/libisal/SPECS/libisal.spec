%define __jar_repack 0
%define _binaries_in_noarch_packages_terminate_build 0
%define _unpackaged_files_terminate_build 0

Name:		libisal
Version:	%{libisal_version}
Release:	%{libisal_release}
Summary:	Intel(R) Intelligent Storage Acceleration Library

Group:		Development/Libraries
License:	bsd 3-clause license
URL:		https://01.org/intel%C2%AE-storage-acceleration-library-open-source-version
Source0:	isa-l-%{libisal_version}.tar.gz
Source1:  do-component-build
Source2:  install_libisal.sh
#BIGTOP_PATCH_FILES

BuildArch:  noarch
Requires:	bash, glibc-devel
Provides: 	libisal
AutoReqProv: 	no

%description
Intel(R) Intelligent Storage Acceleration Library - shared library

%package dev
Summary: Intel(R) Intelligent Storage Acceleration Library - devel files 
Group: Development/Libraries
Requires: libisal
AutoReq: no
%description dev
Intel(R) Intelligent Storage Acceleration Library - devel files

%prep
%setup -q  -n isa-l-%{libisal_version}
#BIGTOP_PATCH_COMMANDS

%build
bash %{SOURCE1}

%install
%__rm -rf $RPM_BUILD_ROOT

/bin/bash %{SOURCE2} $RPM_BUILD_ROOT %{libisal_version}

%files
/usr/lib/libisal.so.2.0.23
/usr/lib/libisal.so.2
/usr/share/doc/libisal/LICENSE

%files dev
/usr/include
/usr/lib/libisal.so

%changelog
