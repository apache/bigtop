%define lib_whirr /usr/lib/whirr
%define man_dir /usr/share/man

# disable repacking jars
%define __os_install_post %{nil}

Name: whirr
Version: %{whirr_version}
Release: %{whirr_release}
Summary: Scripts and libraries for running software services on cloud infrastructure.
URL: http://incubator.apache.org/whirr
Group: Development/Libraries
BuildArch: noarch
Buildroot: %(mktemp -ud %{_tmppath}/%{name}-%{version}-%{release}-XXXXXX)
License: ASL 2.0 
Source0: %{name}-%{whirr_base_version}-incubating-src.tar.gz
Source1: install_%{name}.sh
Source2: whirr.1

# RHEL6 provides natively java
%if 0%{?rhel} == 6
BuildRequires: java-1.6.0-sun-devel
Requires: java-1.6.0-sun
%else
BuildRequires: jdk >= 1.6
Requires: jre >= 1.6
%endif


%description 
Whirr provides

* A cloud-neutral way to run services. You don't have to worry about the
  idiosyncrasies of each provider.
* A common service API. The details of provisioning are particular to the
  service.
* Smart defaults for services. You can get a properly configured system
  running quickly, while still being able to override settings as needed.
    

%prep
%setup -n %{name}-%{whirr_base_version}-incubating

%build

mvn clean source:jar install assembly:assembly -Pjavadoc site

%install
%__rm -rf $RPM_BUILD_ROOT
cp $RPM_SOURCE_DIR/whirr.1 .
sh $RPM_SOURCE_DIR/install_whirr.sh \
          --build-dir=. \
          --prefix=$RPM_BUILD_ROOT

%files 
%defattr(-,root,root)
%attr(0755,root,root) %{lib_whirr}
%attr(0755,root,root) %{_bindir}/%{name}
%attr(0644,root,root) %{man_dir}/man1/whirr.1.gz

