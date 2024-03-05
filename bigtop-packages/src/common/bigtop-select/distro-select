#!/usr/bin/env python2
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
import optparse
import copy
import os
import re
import sys
from params import stack_root, stack_version

# The global prefix and current directory
root = stack_root
bigtop_version = stack_version
current = root + "/current"
lib_root = "/usr/lib"
bin_directory = "/usr/bin"
version_regex = re.compile('[-.]')

spark_wrapper_scripts = ["run-example", "pyspark", "spark-class", "sparkR", "spark-shell", "spark-sql", "spark-submit"]

# The packages and where in the release they should point to
leaves = {
           "hadoop-client": "hadoop",
           "hadoop-hdfs-client": "hadoop-hdfs",
           "hadoop-hdfs-datanode": "hadoop-hdfs",
           "hadoop-hdfs-journalnode": "hadoop-hdfs",
           "hadoop-hdfs-namenode": "hadoop-hdfs",
           "hadoop-hdfs-secondarynamenode": "hadoop-hdfs",
           "hadoop-hdfs-zkfc": "hadoop-hdfs",
           "hadoop-yarn-client": "hadoop-yarn",
           "hadoop-yarn-resourcemanager": "hadoop-yarn",
           "hadoop-yarn-nodemanager": "hadoop-yarn",
           "hadoop-mapreduce-client": "hadoop-mapreduce",
           "hadoop-mapreduce-historyserver": "hadoop-mapreduce",
           "hbase-client": "hbase",
           "hbase-master": "hbase",
           "hbase-regionserver": "hbase",
           "tez-client": "tez",
           "hive-client": "hive",
           "hive-metastore": "hive",
           "hive-server2": "hive",
           "hive-webhcat": "hive-hcatalog",
           "kafka-broker": "kafka",
           "phoenix-client": "phoenix",
           "phoenix-server": "phoenix",
           "zeppelin-server": "zeppelin",
           "zookeeper-client": "zookeeper",
           "zookeeper-server": "zookeeper",
           "solr-server" : "solr",
           "flink-client" : "flink",
           "flink-historyserver" : "flink",
           "spark-client" : "spark",
           "spark-thriftserver" : "spark",
           "spark-historyserver" : "spark",
           "ranger-admin" : "ranger-admin",
           "ranger-usersync" : "ranger-usersync",
           "ranger-tagsync" : "ranger-tagsync",
           "alluxio": "alluxio",
           "alluxio-master": "alluxio",
           "alluxio-worker": "alluxio"
}

# Define the aliases and the list of leaves they correspond to
aliases = {
  "all": leaves.keys(),
  "client" : ["hadoop-client",
              "hbase-client",
              "tez-client",
              "hive-client",
              "phoenix-client",
              "zookeeper-client",
              "flink-client",
              "spark-client"],
  "hadoop-hdfs-server": ["hadoop-hdfs-datanode",
                         "hadoop-hdfs-journalnode",
                         "hadoop-hdfs-namenode",
                         "hadoop-hdfs-zkfc",
                         "hadoop-hdfs-secondarynamenode"],
  "hadoop-mapreduce-server": ["hadoop-mapreduce-historyserver"],
  "hadoop-yarn-server": ["hadoop-yarn-resourcemanager",
                         "hadoop-yarn-nodemanager"],
  "hive-server": ["hive-metastore",
                  "hive-server2",
                  "hive-webhcat"],
}

locked_contexts = {
  "hadoop-client": [("hadoop-hdfs-client", "hadoop-hdfs"),
                    ("hadoop-yarn-client", "hadoop-yarn"),
                    ("hadoop-mapreduce-client", "hadoop-mapreduce"),
                    ("hive-client", "hive"),
                    ("tez-client", "tez"),
                    ("spark-client", "spark")]
}

command_symlinks = {
  "hadoop" : "hadoop-client/../../bin/hadoop",
  "hdfs" : "hadoop-hdfs-client/../../bin/hdfs",
  "yarn" : "hadoop-yarn-client/../../bin/yarn",
  "mapred" : "hadoop-mapreduce-client/../../bin/mapred",
  "hbase" : "hbase-client/../../bin/hbase",
  "beeline" : "hive-client/../../bin/beeline",
  "hive" : "hive-client/../../bin/hive",
  "hiveserver2" : "hive-server2/../../bin/hiveserver2",
  "hcat" : "hive-webhcat/../../bin/hcat",
  "zookeeper-client" : "zookeeper-client/../../bin/zookeeper-client",
  "zookeeper-server" : "zookeeper-server/../../bin/zookeeper-server",
  "zookeeper-server-cleanup" : "zookeeper-server/../../bin/zookeeper-server-cleanup",
  "zookeeper-server-initialize" : "zookeeper-server/../../bin/zookeeper-server-initialize",
  "kafka-console-consumer.sh" : "kafka-broker/../../bin/kafka-console-consumer.sh",
  "kafka-console-producer.sh" : "kafka-broker/../../bin/kafka-console-producer.sh",
  "kafka-run-class.sh" : "kafka-broker/../../bin/kafka-run-class.sh",
  "kafka-topics.sh" : "kafka-broker/../../bin/kafka-topics.sh",
  "solrctl" : "solr-server/../../bin/solrctl",
  "flink" : "flink-client/../../bin/flink",
  "find-spark-home" : "spark-client/../../bin/find-spark-home",
  "spark-example" : "spark-client/../../bin/spark-example",
  "spark-class" : "spark-client/../../bin/spark-class",
  "spark-shell" : "spark-client/../../bin/spark-shell",
  "spark-sql" : "spark-client/../../bin/spark-sql",
  "spark-submit" : "spark-client/../../bin/spark-submit",
  "pyspark" : "spark-client/../../bin/pyspark",
  "alluxio" : "alluxio/bin/alluxio"
}


# Given a package or alias name, get the full list of packages
def getPackages( name ):
  if name == None:
    return leaves.keys()
  if name in aliases:
    return aliases[name]
  if name in leaves:
    return [ name ]

  print("ERROR: Invalid package - " + name)
  print('')
  printPackages()
  sys.exit(1)

# Print the status of each of the given packages
def listPackages( packages ):
  if packages == None:
    packages = leaves

  packages.sort()
  for pkg in packages:
    linkname = current + "/" + pkg
    if os.path.isdir(linkname):
      print(pkg + " - " +
             os.path.basename(os.path.dirname(os.path.dirname(os.path.dirname(os.readlink(linkname))))))
    else:
      print(pkg + " - None")

# Print the avaialable package names
def printPackages():
  packages = leaves.keys()
  packages.sort()
  print("Packages:")
  for pkg in packages:
     print(" " + pkg)
  groups = aliases.keys()
  groups.sort()
  print("Aliases:")
  for pkg in groups:
    print(" " + pkg)

#Print whether given package is supported or not
def isSupportedPacakge(package):
    packages = leaves.keys()
    if package in packages:
      print("%s" % package)
    else:
      print(None)
      sys.exit(1)

# Print the installed packages
def printVersions():
  result = {}
  for f in os.listdir(root):
    if f not in [".", "..", "current", "share", "lost+found"]:
      try:
        result[tuple(map(int, version_regex.split(f)))] = f
      except ValueError:
        print("ERROR: Unexpected file/directory found in %s: %s" % (root, f))
        sys.exit(1)

  keys = result.keys()
  keys.sort()
  for k in keys:
     print(result[k])

# Set the list of packages to the given version
def setPackages(packages, version, rpm_mode):
  if packages == None or version == None:
    print("ERROR: 'set' command must give both package and version")
    print('')
    printHelp()
    sys.exit(1)

  target = root + "/" + version + lib_root
  if not os.path.isdir(target):
    print("ERROR: Invalid version " + version)
    print('')
    print("Valid choices:")
    printVersions()
    sys.exit(1)

  if not os.path.isdir(current):
    os.mkdir(current, 0755)

  packages.sort()
  for pkg in packages:
    linkname = current + "/" + pkg
    if os.path.islink(linkname) and rpm_mode:
      continue
    elif os.path.islink(linkname):
      os.remove(linkname)

    if os.path.exists(linkname):
      print("symlink target %s for %s already exists and it is not a symlink." % (linkname, leaves[pkg]))
      sys.exit(1)

    os.symlink(target + "/" + leaves[pkg], linkname)
    if pkg in locked_contexts:
      for (kid, dir) in locked_contexts[pkg]:
        linkname = current + "/" + kid
        if os.path.islink(linkname):
          os.remove(linkname)
        os.symlink(target + "/" + dir, linkname)

# Create command symlinks
def createCommandSymlinks(packages, rpm_mode):
  work_packages = copy.copy(packages)
  for pkg in packages:
    if pkg in locked_contexts:
      for (child, dir) in locked_contexts[pkg]:
        work_packages.append(child)
  for symlink in command_symlinks:
    pkg = command_symlinks[symlink].split('/')[0]
    filename = bin_directory + "/" + symlink
    target = current + "/" + command_symlinks[symlink]
    if rpm_mode and os.path.lexists(filename):
      continue
    if pkg in work_packages:
      if not os.path.lexists(filename):
        os.symlink(target, filename)
      elif os.path.islink(filename):
        old_value = os.readlink(filename)
        if old_value != target:
          print("WARNING: Replacing link", filename, "from", old_value)
          os.remove(filename)
          os.symlink(target, filename)
      else:
        if os.path.basename(filename) not in spark_wrapper_scripts:
          print("ERROR:", filename, "is a regular file instead of symlink.")
          print('')
          print("Please ensure that the BIGTOP 2.1 (and earlier) packages are")
          print("removed.")

# Do a sanity check on the tables
def sanityCheckTables():
  for alias in aliases:
    for child in aliases[alias]:
      if not child in leaves:
        print("ERROR: Alias", alias, "has bad child", child)
        sys.exit(1)
  locked = set()
  for parent in locked_contexts:
    for (kid, dir) in locked_contexts[parent]:
      locked.add(kid)
  for symlink in command_symlinks:
    parts = command_symlinks[symlink].split('/')
    if not parts[0] in leaves and not parts[0] in locked:
      print("ERROR: command symlink", symlink, "points to an invalid package",\
            parts[0])
      sys.exit(1)

def printHelp():
  print("""
usage: distro-select [-h] [<command>] [<package>] [<version>]

Set the selected version of BIGTOP.

positional arguments:
  <command>   One of set, status, versions, or packages
  <package>   the package name to set
  <version>   the BIGTOP version to set

optional arguments:
  -h, --help  show this help message and exit
  -r, --rpm-mode  if true checks if there is symlink exists and creates the symlink if it doesn't

Commands:
  set      : set the package to a specified version
  status   : show the version of the package
  versions : show the currently installed versions
  packages : show the individual package names
  supports : Check distro-select supports a specific package
""")

def checkCommandParameters(cmd, realLen, rightLen):
  if realLen != rightLen:
    print("ERROR:", cmd, "command takes", rightLen - 1,\
          "parameters, instead of", realLen - 1)
    printHelp()
    sys.exit(1)

 ############################
 #
 # Start of main

sanityCheckTables()

parser = optparse.OptionParser(add_help_option=False)
parser.add_option("-h", "--help", action="store_true", dest="help",
                  help="print help")
parser.add_option("-v", "--version", action="store_true", dest="version",
                  help="print bigtop version")
parser.add_option("-r", "--rpm-mode", action="store_true", dest="rpm_mode", default=False,
                  help="if true checks if there is symlink exists and creates the symlink if it doesn't")
(options, args) = parser.parse_args()

if options.help:
  printHelp()
elif options.version:
  print(bigtop_version)
else:
  if len(args) == 0 or args[0] == 'status':
    if len(args) > 1:
      listPackages(getPackages(args[1]))
    else:
      listPackages(getPackages("all"))
  elif args[0] == "set":
    checkCommandParameters('set', len(args), 3)
    pkgs = getPackages(args[1])
    setPackages(pkgs, args[2], options.rpm_mode)
    createCommandSymlinks(pkgs, options.rpm_mode)
  elif args[0] == "versions":
    checkCommandParameters('versions', len(args), 1)
    printVersions()
  elif args[0] == "packages":
    checkCommandParameters('packages', len(args), 1)
    printPackages()
  elif args[0] == "supports":
    checkCommandParameters('support', len(args), 2)
    isSupportedPacakge(args[1])
  else:
    print("ERROR: Unknown command -", args[0])
    printHelp()
    sys.exit(1)
