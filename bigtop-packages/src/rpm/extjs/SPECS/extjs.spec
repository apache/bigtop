%define __jar_repack 0

%define extjs_install_dir /usr/share/ADH-oozie/

Name:		extjs
Version:	%{extjs_version}
Release:	%{extjs_release}
Summary:	RPM of extjs zip file for oozie

Group:		Application/Server
License:	Apache License, Version 2.0
URL:		  http://extjs.incubator.apache.org/
Source0:	ext-%{extjs_version}.zip
BuildArch:  noarch
Requires:	bash
Provides: 	extjs
AutoReqProv: 	no

%description
This rpm package provides the extjs-2.2.zip for oozie.

%prep

%build

%install
%__rm -rf $RPM_BUILD_ROOT

install -d -m 0755 $RPM_BUILD_ROOT/%{extjs_install_dir}
cp %{SOURCE0} $RPM_BUILD_ROOT/%{extjs_install_dir}

ln -sf %{extjs_install_dir}/ext-%{extjs_version}.zip $RPM_BUILD_ROOT/%{extjs_install_dir}/extjs.zip

%files
%{extjs_install_dir}

%changelog
