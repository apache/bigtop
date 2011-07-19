#!/bin/sh
#
# Copyright 2009 Cloudera, Inc.
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
