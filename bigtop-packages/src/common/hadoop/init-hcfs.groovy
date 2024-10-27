#!/usr/bin/env /usr/lib/bigtop-groovy/bin/groovy
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import groovy.json.JsonSlurper;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.fs.permission.FsPermission;

def final LOG = LogFactory.getLog(this.getClass());
def final jsonParser = new JsonSlurper();

def final USAGE = """\
    *********************************************************************
    USAGE:

        This script provisions the skeleton of a hadoop file system.
    It takes a single argument: The json schema (a list of lists),
    of 4 element tuples.  For an example , see the bigtop init-hcfs.json
    file.  The main elements of the JSON file are:

    A copy of init-hcfs.json ships with bigtop distributions.

    dir: list of dirs to create with permissions.
    user: list of users to setup home dirs with permissions.
    root_user: The root owner of distributed FS, to run shell commands.

    To run this script, you will want to setup your environment using
    init-hcfs.json,
    which defines the properties above, and then invoke this script.

    Details below.

    SETUP YOUR CLUSTER ENVIRONMENT

    As mentinoed above, the init-hcfs.json file is what guides the
    directories/users to setup.
    So first you will want to edit that file as you need to.  Some common
    modifications:


    - Usually the "root_user" on HDFS is just hdfs.  For other file systems
    the root user might be "root".
    - The default hadoop users you may find in the init-hcfs.json template
    you follow "tom"/"alice"/etc.. aren't necessarily on all clusters.

    HOW TO INVOKE:

    1) Simple groovy based method:  Just manually construct a hadoop classpath:

    groovy -classpath /usr/lib/hadoop/hadoop-common-2.0.6-alpha.jar
    :/usr/lib/hadoop/lib/guava-11.0.2.jar
    :/etc/hadoop/conf/:/usr/lib/hadoop/hadoop-common-2.0.6-alpha.jar
    :/usr/lib/hadoop/lib/commons-configuration-1.6.jar
    :/usr/lib/hadoop/lib/commons-lang-2.5.jar:/usr/lib/hadoop/hadoop-auth.jar
    :/usr/lib/hadoop/lib/slf4j-api-1.6.1.jar
    :/usr/lib/hadoop-hdfs/hadoop-hdfs.jar
    :/usr/lib/hadoop/lib/protobuf-java-2.4.0a.jar /vagrant/init-hcfs.groovy
    /vagrant/init-hcfs.json

    2) Another method: Follow the instructions on groovy.codehaus.org/Running
     for setting up groovy runtime environment with
    CLASSPATH and/or append those libraries to the shebang command as
    necessary, and then simply do:

    chmod +x init-hcfs.groovy
    ./init-hcfs.groovy init-hcfs.json

    *********************************************************************
"""

/**
 * The HCFS generic provisioning process:
 *
 *   1) Create a file system skeleton.
 *   2) Create users with home dirs in /user.
 *
 *   In the future maybe we will add more optional steps (i.e. adding libs to
 *   the distribtued cache, mounting FUSE over HDFS, etc...).
 **/

def errors = [
    ("0: No init-hcfs.json input file provided !"): {
      LOG.info("Checking argument length: " + args.length + " " + args);
      return args.length == 1
    },
    ("1: init-hcfs json not found."): {
      LOG.info("Checking for file : " + args[0]);
      return new File(args[0]).exists()
    }];

errors.each { error_message, passed ->
  if (!passed.call()) {
    System.err.println("ERROR:" + error_message);
    System.err.println(USAGE);
    System.exit(1);
  }
}

def final json = args[0];
def final parsedData = jsonParser.parse(new FileReader(json));

/**
 * Groovy  is smart enough to convert JSON
 * fields to objects for us automagically.
 * */
def dirs = parsedData.dir as List;
def users = parsedData.user as List;
def hcfs_super_user = parsedData.root_user;

def final Configuration conf = new Configuration();

LOG.info("Provisioning file system for file system from Configuration: " +
    conf.get("fs.defaultFS"));

/**
 * We create a single FileSystem instance to use for all the file system calls.
 * This script makes anywhere from 20-100 file system operations so it's
 * important to cache and create this only once.
 * */
def final FileSystem fs = FileSystem.get(conf);

LOG.info("PROVISIONING WITH FILE SYSTEM : " + fs.getClass());

// Longest back off time to check whether the file system is ready for write
def final int maxBackOff = 64;

/**
 * Make a  directory.  Note when providing input to this functino that if
 * nulls are given, the commands will work but behaviour varies depending on
 * the HCFS implementation ACLs, etc.
 * @param fs The HCFS implementation to create the Directory on.
 * @param dname Required.
 * @param mode can be null.
 * @param user can be null.
 * @param group can be null,
 */
def mkdir = { FileSystem fsys, Path dname, FsPermission mode, String user, String group ->
  boolean success = false;
  for(i = 1; i <= maxBackOff; i*=2) {
    try {
      success = fsys.mkdirs(dname)
      break;
    } catch(Exception e) {
      LOG.info("Failed to create directory " + dname + "... Retry after " + i + " second(s)");
      Thread.sleep(i*1000);
    }
  }
  if (!success) {
    LOG.info("Can not create directory " + dname + " on " + fsys.getClass());
  }
  if (user != null) {
    fsys.setOwner(dname, user, group);
  }
  if (mode != null) {
    fsys.setPermission(dname, mode);
    FsPermission result = fsys.getFileStatus(dname).getPermission();
    /** Confirm that permission took properly.
     * important to do this since while we work on better
     * docs for modifying and maintaining this new approach
     * to HCFS provisioning.*/
    if (!fsys.getFileStatus(dname).getPermission().equals(mode)) {
      throw new RuntimeException("Failed at setting permission to " + mode +
          "... target directory permission is incorrect: " + result);
    }
  }
}

/**
 * Create a perm from raw string representing an octal perm.
 * @param mode The stringified octal mode (i.e. "1777")
 * */
private FsPermission readPerm(String mode) {
  Short permValue = Short.decode("0" + mode);
  //This constructor will decode the octal perm bits
  //out of the short.
  return new FsPermission(permValue);
}

int dirs_created = 0;
/**
 * Provisioning the directories on the file system.  This is the
 * most important task of this script, as a basic directory skeleton
 * is needed even for basic yarn/mapreduce apps before startup.
 * */
dirs.each() {
  def (dname, mode, user, group) = it;

  dname = new Path(dname);

  //We encode permissions as strings, since they are octal.
  //JSON doesn't support octal natively.
  if (mode != null)
    mode = readPerm(mode) as FsPermission;

  if (user?.equals("HCFS_SUPER_USER"))
    user = hcfs_super_user;

  LOG.info("mkdirs " + dname + " " + user + " " + mode + " " + group);
  mkdir(fs, dname, mode, user, group);

  dirs_created++;
}

LOG.info("Succesfully created " + dirs_created + " directories in the DFS.");

/**
 * Now, for most clusters we will generally start out with at least one
 * user.  You should modify your init-hcfs.json file accordingly if you
 * have a set of users you want to setup for using hadoop.
 *
 * For each user we do initial setup, create a home directory, etc...
 * You may also need to do special tasks if running LinuxTaskControllers,
 * etc, which aren't (yet) handled by this provisioner.
 * */
users.each() {
  def (user, permission, group) = it;
  LOG.info("current user: " + user);
  Path homedir = new Path("/user/" + user);

  //perms should be ALL, RX,RX ^^
  fs.mkdirs(homedir);
  fs.setOwner(homedir, user, group);
  FsPermission perm = readPerm(permission);
  fs.setPermission(homedir, perm);
}


/**
 * Copys jar files from a destination into the distributed FS.
 * Directories and broken symlinks will be skipped.
 *
 * @param fs An instance of an HCFS FileSystem .
 *
 * @param input The LOCAL DIRECTORY containing jar files.
 *
 * @param jarstr A jar file name filter used to reject/accept jar names.
 * See the script below for example of how it's used. Jars matching this
 * string will be copied into the specified path on the "target" directory.
 *
 * @param target The path on the DISTRIBUTED FS where jars should be copied
 * to.
 *
 * @return The total number of jars copied into the DFS.
 */
def copyJars = { FileSystem fsys, File input, String jarstr, Path target ->
  int copied = 0;
  input.listFiles(new FileFilter() {
    public boolean accept(File f) {
      String filename = f.getName();
      boolean validJar = filename.endsWith("jar") && f.isFile();
      return validJar && filename.contains(jarstr)
    }
  }).each({ jar_file ->
    boolean success = false;
    for(i = 1; i <= maxBackOff; i*=2) {
      try {
        fsys.copyFromLocalFile(new Path(jar_file.getAbsolutePath()), target)
        copied++;
        success = true;
        break;
      } catch(Exception e) {
        LOG.info("Failed to upload " + jar_file.getAbsolutePath() + " to " + target + "... Retry after " + i + " second(s)");
        Thread.sleep(i*1000);
      }
      if (!success) {
        LOG.info("Can not upload " + jar_file.getAbsolutePath() + " to " + target + " on " + fsys.getClass());
      }
    }
  });
  return copied;
}

total_jars = 0;

LOG.info("Now copying Jars into the DFS for tez ");
LOG.info("This might take a few seconds...");

def final TEZ_APPS = "/apps";
def final TEZ_HOME = "/usr/lib/tez/";

total_jars += copyJars(fs,
    new File(TEZ_HOME, "lib/"), "",
    new Path(TEZ_APPS, "tez/lib"))

total_jars += copyJars(fs,
    new File(TEZ_HOME), "",
    new Path(TEZ_APPS, "tez"))

LOG.info("Total jars copied into the DFS : " + total_jars);

def tez_tar_gz = TEZ_HOME + "lib/tez.tar.gz"
if (new File(tez_tar_gz).exists()) {
    fs.copyFromLocalFile(new Path(tez_tar_gz), new Path(TEZ_APPS, "tez/lib"))
}
