%define doc_sqoop %{_docdir}/sqoop-%{sqoop_version}
%define lib_sqoop /usr/lib/sqoop


%if  %{?suse_version:1}0

%global initd_dir %{_sysconfdir}/rc.d

%else

%global initd_dir %{_sysconfdir}/rc.d/init.d

%endif


Name: sqoop
Version: %{sqoop_version}
Release: %{sqoop_release}
Summary:   Sqoop allows easy imports and exports of data sets between databases and the Hadoop Distributed File System (HDFS).
URL: http://www.cloudera.com
Group: Development/Libraries
Buildroot: %{_topdir}/INSTALL/%{name}-%{version}
License: APL2
Source0: %{name}-%{sqoop_base_version}.tar.gz
Source1: sqoop-metastore.sh
Source2: sqoop-metastore.sh.suse
Source3: install_sqoop.sh
Buildarch: noarch
BuildRequires:  asciidoc, xmlto
Prereq: hadoop

# RHEL6 provides natively java
%if 0%{?rhel} == 6
BuildRequires: java-1.6.0-sun-devel
Requires: java-1.6.0-sun
%else
BuildRequires: jdk >= 1.6
Requires: jre >= 1.6
%endif

%description 
Sqoop allows easy imports and exports of data sets between databases and the Hadoop Distributed File System (HDFS).

%package metastore
Summary: Shared metadata repository for Sqoop.
URL: http://www.cloudera.com
Group: System/Daemons
Provides: sqoop-metastore
Requires: sqoop = %{version}-%{release} 

%if  %{?suse_version:1}0
# Required for init scripts
Requires: insserv
%else
# Required for init scripts
Requires: redhat-lsb
%endif


%description metastore
Shared metadata repository for Sqoop. This optional package hosts a metadata
server for Sqoop clients across a network to use.

%prep
%setup -n sqoop-%{sqoop_base_version}

%build
ant -f build.xml package -Dversion=%{version}

%install
%__rm -rf $RPM_BUILD_ROOT
sh $RPM_SOURCE_DIR/install_sqoop.sh \
          --build-dir=. \
          --doc-dir=%{doc_sqoop} \
          --prefix=$RPM_BUILD_ROOT

%__install -d -m 0755 $RPM_BUILD_ROOT/usr/bin
%__install -d -m 0755 $RPM_BUILD_ROOT/%{initd_dir}/

%__rm -f $RPM_BUILD_ROOT/%{lib_sqoop}/lib/hadoop-mrunit*.jar

%if  %{?suse_version:1}0
orig_init_file=$RPM_SOURCE_DIR/sqoop-metastore.sh.suse
%else
orig_init_file=$RPM_SOURCE_DIR/sqoop-metastore.sh
%endif

init_file=$RPM_BUILD_ROOT/%{initd_dir}/sqoop-metastore
%__cp $orig_init_file $init_file
chmod 0755 $init_file

%__install -d  -m 0755 $RPM_BUILD_ROOT/var/lib/sqoop
%__install -d  -m 0755 $RPM_BUILD_ROOT/var/log/sqoop

%pre
getent group sqoop >/dev/null || groupadd -r sqoop
getent passwd sqoop > /dev/null || useradd -c "Sqoop" -s /sbin/nologin \
	-g sqoop -r -d /var/lib/sqoop sqoop 2> /dev/null || :

%post metastore
chkconfig --add sqoop-metastore

%preun metastore
service sqoop-metastore stop
chkconfig --del sqoop-metastore

%files metastore
%attr(0755,root,root) %{initd_dir}/sqoop-metastore
%attr(0755,sqoop,sqoop) /var/lib/sqoop
%attr(0755,sqoop,sqoop) /var/log/sqoop

# Files for main package
%files 
%defattr(0755,root,root)
%{lib_sqoop}
%{_sysconfdir}/sqoop/conf
%{_bindir}/sqoop
%{_bindir}/sqoop-codegen
%{_bindir}/sqoop-create-hive-table
%{_bindir}/sqoop-eval
%{_bindir}/sqoop-export
%{_bindir}/sqoop-help
%{_bindir}/sqoop-import
%{_bindir}/sqoop-import-all-tables
%{_bindir}/sqoop-job
%{_bindir}/sqoop-list-databases   
%{_bindir}/sqoop-list-tables
%{_bindir}/sqoop-metastore
%{_bindir}/sqoop-version
%{_bindir}/sqoop-merge

%defattr(0644,root,root,0755)
%{lib_sqoop}/lib/
%{lib_sqoop}/*.jar
%{_mandir}/man1/sqoop.1.gz
%{_mandir}/man1/sqoop-codegen.1.gz
%{_mandir}/man1/sqoop-create-hive-table.1.gz
%{_mandir}/man1/sqoop-eval.1.gz
%{_mandir}/man1/sqoop-export.1.gz
%{_mandir}/man1/sqoop-help.1.gz
%{_mandir}/man1/sqoop-import-all-tables.1.gz
%{_mandir}/man1/sqoop-import.1.gz
%{_mandir}/man1/sqoop-job.1.gz
%{_mandir}/man1/sqoop-list-databases.1.gz
%{_mandir}/man1/sqoop-list-tables.1.gz 
%{_mandir}/man1/sqoop-metastore.1.gz
%{_mandir}/man1/sqoop-version.1.gz
%{_mandir}/man1/sqoop-merge.1.gz
%{_docdir}/sqoop-%{sqoop_version}

