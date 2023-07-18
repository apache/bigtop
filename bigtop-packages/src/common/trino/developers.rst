For Developers: Building a component from Git repository

Prerequisites

You will need git installed.
You will need java 11 installed.
You will need to use gradlew which is included in the source code. (Right in the root of the project folder)
This project's gradlew has more documentation here
Use git to download BigTop :
git clone https://github.com/apache/bigtop.git

move into the root project folder:
cd bigtop

To fetch source from a Git repository, there're two ways to achieve this: a). modify ./bigtop.bom and add JSON snippets to your component/package, or b). specify properties at command line

bigtop.bom
Add following JSON snippets to the desired component/package:

git     { repo = ""; ref = ""; dir = ""; commit_hash = "" }
repo - SSH, HTTP or local path to Git repo.
ref - branch, tag or commit hash to check out.
dir - [OPTIONAL] directory name to write source into.
commit_hash - [OPTIONAL] a commit hash to reset to.
Some packages have different names for source directory and source tarball (hbase-0.98.5-src.tar.gz contains hbase-0.98.5 directory). By default source will be fetched in a directory named by tarball { source = TARBALL_SRC } without .t* extension. To explicitly set directory name use the dir option.

When commit_hash specified, the repo to build the package will be reset to the commit hash.

Example for trino:

     name    = 'trino'
     relNotes = 'trino is an open source distributed SQL query engine'
     version { base = '345.0'; pkg = base; vdp_version_with_bn = '3.2.2.0-1'; release = 1 }
     git     { repo = "https://github.trusted.visa.com/opensource/trino.git"
                                    ref = "345.0"
                                    dir = "$name-${version.base}.${version.vdp_version_with_bn}" }
