# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements. See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License. You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

install

repo --name=fedora --mirrorlist=http://mirrors.fedoraproject.org/mirrorlist?repo=fedora-$releasever&arch=$basearch
repo --name=updates --mirrorlist=http://mirrors.fedoraproject.org/mirrorlist?repo=updates-released-f$releasever&arch=$basearch

#trunk
repo --name=bigtop --baseurl=http://bigtop01.cloudera.org:8080/job/Bigtop-hadoop-0.23-Repository/label=fedora16/lastSuccessfulBuild/artifact/repo/ --noverifyssl

#RCs
#repo --name=bigtop --baseurl=http://bigtop01.cloudera.org:8080/job/Bigtop-RCs-Repository/./label=fedora16//lastSuccessfulBuild/artifact/repo/

lang en_US.UTF-8
keyboard us
rootpw --plaintext bigtop
firewall --disabled
authconfig --enableshadow --enablemd5
selinux --disabled
timezone --utc America/Los_Angeles
xconfig --startxonboot
part / --size 4096 --fstype ext4

%packages
@base
@base-x
@core
@java
@kde-desktop
@admin-tools
@graphical-internet
@hardware-support
@fonts
@input-methods
#@development-tools
#@development-libs
#@engineering-and-scientific
#-gnome-*
-verne-backgrounds-gnome
-transmission-*
-opencv 
-marble-*
-marble
-kdegames-*
-k3b*
-akonadi*
-scribus
grub-efi
grub2
efibootmgr

vim-enhanced
vim-X11

hadoop
hadoop-conf-pseudo
hbase
hbase-master
hbase-doc
hbase-regionserver
hbase-thrift
hive
hive-metastore
hive-server
sqoop
sqoop-metastore
flume
flume-node
mahout
pig
oozie
oozie-client
whirr
zookeeper
zookeeper-server
%end
%post

/usr/sbin/adduser --create-home bigtop
passwd -d bigtop

echo 'install ipv6 /bin/true' >> /etc/modprobe.d/disable-ipv6.conf
echo 'bigtop    ALL=NOPASSWD:    ALL' >> /etc/sudoers

cat > /etc/sysconfig/desktop <<EOF
DESKTOP="KDE"
DISPLAYMANAGER="KDE"
EOF

cat > /home/bigtop/.dmrc <<EOF
[Desktop]
Language=en_US.utf8
Layout=us
Session=kde-plasma
EOF
/bin/chmod 644 /home/bigtop/.dmrc
/bin/chown bigtop:bigtop /home/bigtop/.dmrc

sed -ie 's/#AutoLoginEnable=true/AutoLoginEnable=true/' /etc/kde/kdm/kdmrc
sed -ie 's/#AutoLoginUser=.*/AutoLoginUser=bigtop/' /etc/kde/kdm/kdmrc
sed -i 's/#PreselectUser=Default/PreselectUser=Default/' /etc/kde/kdm/kdmrc
sed -i 's/#DefaultUser=.*/DefaultUser=bigtop/' /etc/kde/kdm/kdmrc

mkdir -p /home/bigtop/.kde/share/config/
cat > /home/bigtop/.kde/share/config/nepomukserverrc <<EOF
[Basic Settings]
Start Nepomuk=false
[Service-nepomukstrigiservice]
autostart=false
EOF

mkdir -p /home/bigtop/.kde/share/config/
cat > /home/bigtop/.kde/share/config/apper <<EOF
[CheckUpdate]
autoUpdate=0
interval=0
EOF

mkdir -p /home/bigtop/Desktop
cat > /home/bigtop/Desktop/HDFS <<EOF
#!/usr/bin/env xdg-open
[Desktop Entry]
Comment[en_US]=
Comment=
Exec=/usr/bin/firefox http://localhost:50070/
GenericName[en_US]=HDFS
GenericName=HDFS
MimeType=
Name[en_US]=HDFS
Name=HDFS
Path=
StartupNotify=false
Terminal=false
TerminalOptions=
Type=Application
X-DBUS-ServiceName=
X-DBUS-StartupType=
X-KDE-SubstituteUID=false
X-KDE-Username=
EOF

cat > /home/bigtop/Desktop/MapReduce <<EOF
#!/usr/bin/env xdg-open
[Desktop Entry]
Comment[en_US]=
Comment=
Exec=/usr/bin/firefox http://localhost:50030/
GenericName[en_US]=MapReduce
GenericName=MapReduce
MimeType=
Name[en_US]=MapReduce
Name=MapReduce
Path=
StartupNotify=false
Terminal=false
TerminalOptions=
Type=Application
X-DBUS-ServiceName=
X-DBUS-StartupType=
X-KDE-SubstituteUID=false
X-KDE-Username=
EOF

cat > /home/bigtop/Desktop/HBase <<EOF
#!/usr/bin/env xdg-open
[Desktop Entry]
Comment[en_US]=
Comment=
Exec=/usr/bin/firefox http://localhost:60010/
GenericName[en_US]=HBase
GenericName=HBase
MimeType=
Name[en_US]=HBase
Name=HBase
Path=
StartupNotify=false
Terminal=false
TerminalOptions=
Type=Application
X-DBUS-ServiceName=
X-DBUS-StartupType=
X-KDE-SubstituteUID=false
X-KDE-Username=
EOF

cat > /home/bigtop/Desktop/Oozie <<EOF
#!/usr/bin/env xdg-open
[Desktop Entry]
Comment[en_US]=
Comment=
Exec=/usr/bin/firefox http://localhost:11000/
GenericName[en_US]=Oozie
GenericName=Oozie
MimeType=
Name[en_US]=Oozie
Name=Oozie
Path=
StartupNotify=false
Terminal=false
TerminalOptions=
Type=Application
X-DBUS-ServiceName=
X-DBUS-StartupType=
X-KDE-SubstituteUID=false
X-KDE-Username=
EOF


cat > /etc/hbase/conf/hbase-site.xml <<EOF
<configuration>
	<property>
		<name>hbase.cluster.distributed</name>
		<value>true</value>
	</property>
	<property>
		<name>hbase.zookeeper.quorum</name>
		<value>localhost</value>
	</property>
	<property>
		<name>hbase.rootdir</name>
		<value>hdfs://localhost:8020/hbase</value>
	</property>
</configuration>
EOF

chown -R bigtop:bigtop /home/bigtop/

systemctl disable firstboot-text.service
systemctl disable firstboot-graphical.service

/usr/bin/yes Y | su hdfs /bin/bash -c '/usr/bin/hadoop namenode -format'
%end
