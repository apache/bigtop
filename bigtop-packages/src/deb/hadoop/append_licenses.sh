#!/bin/bash
#
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
#
# Debian prefers all license information to go in /usr/share/doc/copyright
# rather than individual files in /usr/lib/hadoop. This script appends all
# the licenses to the target file and removes the originals.

set -e

is_apache_2() {
    head $1 | perl -n0 -e 'exit(!(m/Apache/ && m/Version 2\.0/))'
}

out_file=debian/hadoop/usr/share/doc/hadoop/copyright

for license in `find debian/hadoop/usr/lib/hadoop/ -name \*LICENSE.txt` ; do
    (echo
     echo -------------
     echo Included license: $(basename $license)
     echo -------------
     echo
     # Check if it's apache 2.0, since lintian gets grumpy if you include
     # the full text
     if is_apache_2 $license ; then
       echo 'Apache 2.0 License - see /usr/share/common-licenses/Apache-2.0'
     else
       cat $license
     fi
     ) >> $out_file
    rm $license
done
