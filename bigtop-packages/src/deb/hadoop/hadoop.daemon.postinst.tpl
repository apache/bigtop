#!/bin/sh
set -e
if [ -x "/etc/init.d/hadoop-@HADOOP_DAEMON@" ]; then
	update-rc.d hadoop-@HADOOP_DAEMON@ defaults >/dev/null
	if [ -x "`which invoke-rc.d 2>/dev/null`" ]; then
		invoke-rc.d hadoop-@HADOOP_DAEMON@ start || :
	else
		/etc/init.d/hadoop-@HADOOP_DAEMON@ start || :
	fi
fi
