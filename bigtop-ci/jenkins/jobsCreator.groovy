/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// FIXME: it would be nice to extract the following from bigtop.mk on the fly
def bigtopComponents = ["bigtop-groovy", "bigtop-jsvc", "bigtop-tomcat", "bigtop-utils",
                        "zookeeper", "hadoop", "hbase", "hive", "pig",
                        "flume", "giraph", "ignite-hadoop", "oozie", "phoenix",
                        "solr", "spark", "sqoop", "alluxio", "whirr"]
// FIXME: it would be nice to extract the following from some static configuration file
def targetOS = ["fedora-20",  "opensuse-12.3",  "ubuntu-14.04"]
def gitUrl = "https://gitbox.apache.org/repos/asf/bigtop.git"
def gitBranch = "master"
def dockerLabel = "docker"
def jobPrefix="Bigtop"
def pkgTestsuites =["TestPackagesBasicsWithRM", "TestPackagesPseudoDistributedServices", 
                    "TestPackagesPseudoDistributedDependency", "TestPackagesPseudoDistributedFileContents", 
                    "TestPackagesPseudoDistributedWithRM", "TestPackagesBasics"]

job {
    name "${jobPrefix}-${gitBranch}-All"
    description "Top level job that kicks off everything for a complete Bigtop CI run on branch ${gitBranch}"
    logRotator(7 /*days to keep */, 10 /* # of builds */, 7 /*days to keep */, 10 /* # of builds */) 
    label('master')
    triggers {
      cron("0 3 * * *")
    }

    steps {
       downstreamParameterized {
          trigger("${jobPrefix}-${gitBranch}-" + bigtopComponents.join("-pkg,${jobPrefix}-${gitBranch}-") + "-pkg",
                     'UNSTABLE_OR_BETTER', true, ["buildStepFailure": "FAILURE", "failure": "FAILURE", "unstable": "UNSTABLE"]) {
            currentBuild()
          }
          
           trigger("${jobPrefix}-${gitBranch}-Repository",
                     'UNSTABLE_OR_BETTER', true, ["buildStepFailure": "FAILURE", "failure": "FAILURE", "unstable": "UNSTABLE"]) {
            currentBuild()
          }

          trigger("${jobPrefix}-${gitBranch}-Packagetest",
                     'UNSTABLE_OR_BETTER', true, ["buildStepFailure": "FAILURE", "failure": "FAILURE", "unstable": "UNSTABLE"]) {
            currentBuild()
          }
       }
    }
}

job(type: Matrix) {
    name "${jobPrefix}-${gitBranch}-Repository"
    description "Top level job that creates final repository from packages built off of ${gitBranch} branch for all the matrix"
    logRotator(2 /*days to keep */, 2 /* # of builds */, 2 /*days to keep */, 2 /* # of builds */) 
    label('master')

    steps {
      shell('''
#!/bin/bash -ex
env
rm -rf * /var/tmp/* || :

# By default Jenkins uses an internal (unresolvable) EC2 DNS name
JOB_URL="${JOB_URL/#*:/http://bigtop01.cloudera.org:}"

export PROJECTS="''' + bigtopComponents.join(' ') + '''"

mkdir -p packages
pushd packages
  for project in $PROJECTS; do
    mkdir -p ${project}
    pushd ${project}
      wget "http://bigtop01.cloudera.org:8080/job/${jobPrefix}-${gitBranch}-${project}-pkg/TARGET_OS=${TARGET_OS},slaves=docker/lastSuccessfulBuild/artifact/*zip*/archive.zip"
      unzip archive.zip
      rm archive.zip
    popd
  done
popd

if [ -n "find packages -iname '*.src.rpm'`" ] ; then
  mkdir -p repo/{RPMS,SRPMS}
  mv `find packages -iname "*.src.rpm"` repo/SRPMS/
  mv `find packages -iname "*.rpm"` repo/RPMS/

  pushd repo
    createrepo .
  popd
  cat > repo/bigtop.repo << __EOT__
[bigtop]
name=Bigtop
enabled=1
gpgcheck=0
type=NONE
baseurl=${JOB_URL}/lastSuccessfulBuild/artifact/repo/
__EOT__


else
  mkdir -p repo/conf
  cat > repo/conf/distributions <<__EOT__
Origin: Bigtop
Label: Bigtop
Suite: stable
Codename: bigtop
Version: 0.3
Architectures: i386 amd64 source
Components: contrib 
Description: Bigtop
__EOT__
  for i in `find packages -name \\*.changes` ; do
    reprepro -Vb repo include bigtop $i
  done
  echo "deb ${JOB_URL}/lastSuccessfulBuild/artifact/repo/ bigtop contrib" > repo/bigtop.list
fi
              ''')
    }

    publishers {
        archiveArtifacts('repo/**/*')
    }

    axes {
        text('TARGET_OS', targetOS)
        label('slaves', dockerLabel)
    }
}

job(type: Matrix) {
    name "${jobPrefix}-${gitBranch}-Packagetest"
    description "Runs smoke tests on all packages built off of ${gitBranch} branch for all the matrix"
    logRotator(4 /*days to keep */, 4 /* # of builds */, 4 /*days to keep */, 4 /* # of builds */) 
    label('master')

    parameters {
      choiceParam("PKG_SUITE", pkgTestsuites)
    }

    scm {
      git { node -> // is hudson.plugins.git.GitSCM
        // node / gitConfigName('Bigtop')
        // node / gitConfigEmail('dev@bigtop.apache.org')

        remote {
          name(gitBranch)
          url(gitUrl)
        }
        branch(gitBranch)
      }
    }    

    steps {
      shell('''
#!/bin/bash

# Working around SuSE madness
rm -f /etc/zypp/repos.d/*Cloud* || :

export HADOOP_HOME=/usr/lib/hadoop
export HADOOP_CONF_DIR=/etc/hadoop/conf
export REPO_FILE_URL="http://bigtop01.cloudera.org:8080/view/Bigtop-trunk/job/Bigtop-trunk-Repository/label=${label/-slave/}/lastSuccessfulBuild/artifact/repo/bigtop"
export REPO_KEY_URL="http://archive.apache.org/dist/incubator/bigtop/bigtop-0.3.0-incubating/repos/GPG-KEY-bigtop"

if [ "$label" = "precise-slave" -o "$label" = "quetzal-slave" -o "$label" = "trusty-slave" ]; then
  REPO_FILE_URL="${REPO_FILE_URL}.list"
else
  REPO_FILE_URL="${REPO_FILE_URL}.repo"
fi

docker run -u `id -u` -e HOME=/var/lib/jenkins -e BIGTOP_BUILD_STAMP=.${BUILD_NUMBER}   \\
                               -v `pwd`/build/home:/var/lib/jenkins \\
                               -v `pwd`:/ws bigtop/slaves:$TARGET_OS \\
                               bash -c '. /etc/profile.d/bigtop.sh; cd /ws ; mvn \\
  -f bigtop-tests/test-execution/package/pom.xml \\
  clean verify                                   \\
  -Dbigtop.repo.file.url="'${REPO_FILE_URL}'"      \\
  -Dorg.apache.bigtop.itest.log4j.level=TRACE    \\
  -Dlog4j.debug=true                             \\
  -Dorg.apache.maven-failsafe-plugin.testInclude="**/'${PKG_SUITE}'.*"
              ''')
    }

    publishers {
        archiveJunit('**/bigtop-tests/test-execution/package/target/failsafe-reports/*.xml') {
            retainLongStdout()
        }
    }

    axes {
        text('TARGET_OS', targetOS)
        label('slaves', dockerLabel)
    }
}

bigtopComponents.each { comp->
  job(type: Matrix) {
    println comp
    name "${jobPrefix}-${gitBranch}-${comp}-pkg"
    description "Builds packages on every platform of the matrix according to the ${gitBranch} branch"
    logRotator(7 /*days to keep */, 10 /* # of builds */, 7 /*days to keep */, 10 /* # of builds */) 
    label('master')

    scm {
      git { node -> // is hudson.plugins.git.GitSCM
        // node / gitConfigName('Bigtop')
        // node / gitConfigEmail('dev@bigtop.apache.org')

        remote {
          name(gitBranch)
          url(gitUrl)
        }
        branch(gitBranch)
      }
    }

// export JAVA_OPTS="-Xmx1536m -Xms256m -XX:MaxPermSize=256m"
// export MAVEN_OPTS="-Xmx1536m -Xms256m -XX:MaxPermSize=256m"
//
// if [ -e /etc/SuSE-release ] ; then
//  export LDFLAGS="-lrt"
// fi
    steps {
      shell('''
              mkdir -p build/home || :
              docker run -u `id -u` -e HOME=/var/lib/jenkins -e BIGTOP_BUILD_STAMP=.${BUILD_NUMBER}   \\
                               -v `pwd`/build/home:/var/lib/jenkins \\
                               -v `pwd`:/ws bigtop/slaves:$TARGET_OS \\
                               bash -c '. /etc/profile.d/bigtop.sh; cd /ws ; gradle ''' + "'\"${comp}-pkg\"")
    }

    publishers {
        archiveArtifacts('output/**/*/*')
    }

    axes {
        text('TARGET_OS', targetOS)
        label('slaves', dockerLabel)
    }
  }
}
