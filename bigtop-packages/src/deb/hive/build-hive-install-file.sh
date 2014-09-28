#!/bin/bash
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

# This script is meant to include only libraries in the main hive package not already included in other hive subpackages.
# It does this by listing the contents of hive/lib into a temporary file and then it will
# lists all libs in in the .install files for other subpackages, adding those lines to a tmp exclude file
# It will then do a grep diff between the list of hive/lib and the exclude file and only include lines not found in the exclude
# It then reads what should be included to stdout as well as some files that should also be in the package, which is listed in hive.install.include.
for i in debian/tmp/usr/lib/hive/lib/*
do
	echo ${i} >> debian/hive.include
done

get_excludes () {
	while read line
	do
		echo ${line:0:(-5)} >> debian/hive.exclude			# get rid of *.jar and add to excludes list
	done <$1												# Read in .install file for subpackage
}

# Exclude all libraries listed in these files.
get_excludes "debian/hive-jdbc.install"
get_excludes "debian/hive-hbase.install"

# Exclude the libraries from above and put the remaining libs in hive.install.
grep -Fv -f debian/hive.exclude debian/hive.include
# Add extra files that aren't libs.
cat debian/hive.install.include
