#!/usr/bin/python

'''
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
'''

import os
import re
import warnings
from optparse import OptionParser

def main():
  parser = OptionParser()
  parser.add_option("-d", "--directory", help="Top level directory of source tree")
  parser.add_option("-r", "--report", help="API compatibility report file, in HTML format")

  (options, args) = parser.parse_args()

  # Get the ATS endpoint if it's not given.
  if options.directory == None:
    print "You must specify a top level directory of the source tree"
    return 1

  if options.report == None:
    print "You must specify the report to check against"
    return 1

  publicClasses = set()
  for directory in os.walk(options.directory):
    for afile in directory[2]:
      if re.search("\.java$", afile) != None:
        handle = open(os.path.join(directory[0], afile))
        # Figure out the package we're in
        pre = re.search("org/apache/hadoop[\w/]*", directory[0])
        if pre == None:
           warnings.warn("No package for " + directory[0])
           continue
        package = pre.group(0)
        expecting = 0
        for line in handle:
          if re.search("@InterfaceAudience.Public", line) != None:
            expecting = 1
          classname = re.search("class (\w*)", line)
          if classname != None and expecting == 1:
            publicClasses.add(package + "/" + classname.group(1))
            expecting = 0
        handle.close()

  handle = open(options.report)
  haveChecked = set()
  for line in handle:
    classre = re.search("mangled: <b>(org/apache/hadoop[\w/]+)", line)
    if classre != None:
      classname = classre.group(1)
      if classname not in haveChecked:
        if classname in publicClasses:
          print "Warning, found change in public class " + classname
        haveChecked.add(classname)
  handle.close()
  



main()

      
