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

package org.apache.bigtop.itest.pmanager

import org.apache.bigtop.itest.shell.Shell
import org.apache.bigtop.itest.shell.OS
import org.apache.bigtop.itest.posix.Service

public abstract class PackageManager {
  /**
   * Set package manager specific default values
   *
   * @param defaults String of default values encoded in a package manager specific way
   */
  abstract public void setDefaults(String defaults)
  /**
   * Register a binary package repository so that packages can be accessed from it.
   * NOTE: repository management is assumed to follow a KVP API with unique implementation
   * specific keys (records) referencing tuples of information describing a repository
   *
   * @param record a package manager specific KEY portion of the repository registration (null is default)
   * @param url a URL containing the packages constituting the repository (null is default)
   * @param key an optional (can be null) cryptographic key for authenticating the content of the repository
   * @param cookie an optional, package manager specific opaque string
   * @return int return code of the operation: o in case of success, non-zero otherwise
   */
  abstract
  public int addBinRepo(String record, String url, String key, String cookie)
  /**
   * Register a binary package repository so that packages can be accessed from it.
   * NOTE: repository management is assumed to follow a KVP API with unique implementation
   * specific keys (records) referencing tuples of information describing a repository
   *
   * @param record a package manager specific KEY portion of the repository registration (null is default)
   * @param url a URL containing the packages constituting the repository (null is default)
   * @param key an optional (can be null) cryptographic key for authenticating the content of the repository
   * @return int return code of the operation: o in case of success, non-zero otherwise
   */
  public int addBinRepo(String record, String url, String key) {
    addBinRepo(record, url);
  }
  /**
   * Register a binary package repository so that packages can be accessed from it.
   * NOTE: repository management is assumed to follow a KVP API with unique implementation
   * specific keys (records) referencing tuples of information describing a repository
   *
   * @param record a package manager specific KEY portion of the repository registration (null is default)
   * @param descr a full description of the repository in a native format
   * @return int return code of the operation: o in case of success, non-zero otherwise
   */
  public int addBinRepo(String record, String descr) {
    Shell superWriter = new Shell("/bin/dd of=${String.format(getRepository_registry(), record)}", "root");
    superWriter.exec("${descr}");
    return superWriter.getRet();
  }
  /**
   * Clean up the repository cache
   * @return int return code of the operation: o in case of success, non-zero otherwise
   */
  abstract public int cleanup()

  /**
   * Refresh the cached data describing the content of all registered repositories
   * @return int return code of the operation: o in case of success, non-zero otherwise
   */
  abstract public int refresh()

  /**
   * De-register a binary package repository.
   *
   * @param record a package manager specific KEY portion of the repository registration (null is default)
   * @return int return code of the operation: o in case of success, non-zero otherwise
   */
  public int removeBinRepo(String record) {
    shRoot.exec("rm -f ${String.format(getRepository_registry(), record)}");
    return shRoot.getRet();
  }
  /**
   * Search for a package in all registered repositories
   *
   * @param name name of the package (inexact matches are ok)
   * @return list of matching packages found in all registered repositories (can be empty)
   */
  abstract public List<PackageInstance> search(String name)
  /**
   * Search for a package in all registered repositories
   *
   * @param name name of the package (inexact matches are ok)
   * @return list of matching packages found in all registered repositories (can be empty)
   */
  abstract public List<PackageInstance> lookup(String name)
  /**
   * Install a given package (from collection of all the packages available in all the repositories)
   *
   * @param pkg a package to be installed
   * @return int return code of the operation: o in case of success, non-zero otherwise
   */
  abstract public int install(PackageInstance pkg)
  /**
   * Remove a given package that is already installed on the system
   *
   * @param pkg a package to be installed
   * @return int return code of the operation: o in case of success, non-zero otherwise
   */
  abstract public int remove(PackageInstance pkg)
  /**
   * Check if a given package is installed on the system
   *
   * @param pkg a package to be checked
   * @return true if the package is installed and can be used, false otherwise
   */
  abstract public boolean isInstalled(PackageInstance pkg)

  /**
   * Get a list of services (System V init scripts) provided by a given package
   *
   * @param pkg a package that is expected to provide 0, 1 or multiple services
   * @return list of Service instances
   */
  public Map<String, Service> getServices(PackageInstance pkg) {
    return pkg.getServices();
  }

  /**
   * List a content of a given package
   *
   * @param pkg a package that is expected to provide >1 entry in its content
   * @return list file and directory names belong to the package.
   */
  public List<String> getContentList(PackageInstance pkg) {
    return pkg.getFiles();
  }

  /**
   * List config files in a given package
   *
   * @param pkg a package in question
   * @return list config file names that belong to the package.
   */
  public List<String> getConfigs(PackageInstance pkg) {
    return pkg.getConfigs();
  }

  /**
   * List documentation files in a given package
   *
   * @param pkg a package in question
   * @return list documentation file names that belong to the package.
   */
  public List<String> getDocs(PackageInstance pkg) {
    return pkg.getDocs();
  }

  /**
   * type of a package manager. expected to be overwritten by concrete subclasses implementing
   * particular package managers (yum, apt, zypper, etc.)
   */
  String type = "abstract"

  /**
   * A registry location for repositories to be added to. Currently all the package managers
   * we have to support can be handled by treating this as a subdirectory in a local filesystem.
   */
  String repository_registry = "/tmp/%s.repo"

  Shell shRoot = new Shell("/bin/bash -s", "root")
  Shell shUser = new Shell("/bin/bash -s")

  /**
   * Returns a concrete implementation of PackageManager specific for the distro
   * where the code is executed (e.g. this OS)
   * @return instance of a concrete implementation of PackageManager
   */
  static public PackageManager getPackageManager() {
    return getPackageManager("");
  }

  /**
   * Operate on services provided by a package (start, stop, status, restart)
   * NOTE: this method assumes that a given package bundles a number of services (daemons)
   * and allows you to operate on these services without requiring an explicit knowledge
   * of their names. If a single package provides multiple services all of them will
   * be operated on simultaneously (you don't get to choose any subsets). If a package
   * doesn't provide any services calling this method is a noop.
   *
   * @param pkg a package that is expected to provide 0, 1 or multiple services
   * @param action what to do with service(s) (start, stop, status, restart)
   * @deprecated it is now recommended to use getServices() instead
   */
  @Deprecated
  public void svc_do(PackageInstance pkg, String action) {
    pkg.getServices().each {
      it."$action"()
    }
  }

  /**
   * Returns a concrete implementation of PackageManager specific for a given linux
   * flavor.
   * @param linux_flavor e.g. ubuntu, debian, redhat, centos, etc.
   * @return instance of a concrete implementation of PackageManager
   */
  static public PackageManager getPackageManager(String linux_flavor) {
    switch (linux_flavor ?: OS.linux_flavor) {
      case ~/(?is).*(ubuntu|debian).*/:
        return new AptCmdLinePackageManager();
      case ~/(?is).*(redhat|centos|rhel|fedora|enterpriseenterpriseserver).*/:
        return new YumCmdLinePackageManager();
      case ~/(?is).*(suse|sles|sled).*/:
        return new ZypperCmdLinePackageManager();
      case ~/(?is).*(mageia).*/:
        return new UrpmiCmdLinePackageManager();
      default:
        return null;
    }
  }
}
