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

import os

from jujubigdata import utils
from path import Path

from charms.reactive import is_state, when, when_not, set_state
from charms.layer.apache_bigtop_base import Bigtop, get_package_version
from charmhelpers.core import hookenv


def get_good_jars(dir, prefix=True):
    """
    Walk a directory (non-recursively) and return a list of (good) jars.

    Some jars included in giraph have classes that are incompatible with yarn
    nodemanagers. Filter these out when constructing a list of good
    jars. If omitting the entire .jar is too coarse, the jar will need to be
    reconstructed with the offending .class removed.

    param: str dir: Directory to walk
    param: bool prefix: When true, prepend the directory to each jar entry
    """
    # Known incompatible jars:
    # - hive-exec-0.11.0 protobuf class
    #   java.lang.VerifyError: ... overrides final method getUnknownFields
    bad_jars = ['hive-exec-0.11.0.jar']
    good_jars = []
    for file in os.listdir(dir):
        if file.endswith('.jar') and file not in bad_jars:
            good_jars.append(Path(dir / file) if prefix else file)

    return good_jars


@when('bigtop.available')
def report_status():
    """Set juju status based on the deployment topology."""
    giraph_joined = is_state('giraph.joined')
    giraph_installed = is_state('giraph.installed')
    if not giraph_joined:
        hookenv.status_set('blocked',
                           'waiting for relation to a giraph host')
    elif giraph_installed:
        hookenv.status_set('active',
                           'ready')


@when('bigtop.available', 'giraph.joined')
@when_not('giraph.installed')
def install_giraph(giraph):
    """Install giraph when prerequisite states are present."""
    hookenv.status_set('maintenance', 'installing giraph')
    bigtop = Bigtop()
    bigtop.render_site_yaml(
        roles=[
            'giraph-client',
        ],
    )
    bigtop.trigger_puppet()
    giraph_home = Path('/usr/lib/giraph')
    giraph_libdir = Path(giraph_home / 'lib')
    giraph_examples = Path('{}/resources/giraph-examples-1.1.0.jar'.format(
        hookenv.charm_dir()))

    # Gather a list of all the giraph jars (needed for -libjars)
    giraph_jars = [giraph_examples]
    giraph_jars.extend(get_good_jars(giraph_home, prefix=True))
    giraph_jars.extend(get_good_jars(giraph_libdir, prefix=True))

    # Update environment with appropriate giraph bits. HADOOP_CLASSPATH can
    # use wildcards (and it should for readability), but GIRAPH_JARS, which
    # is intended to be used as 'hadoop jar -libjars $GIRAPH_JARS', needs to
    # be a comma-separate list of jars.
    with utils.environment_edit_in_place('/etc/environment') as env:
        cur_cp = env['HADOOP_CLASSPATH'] if 'HADOOP_CLASSPATH' in env else ""
        env['GIRAPH_HOME'] = giraph_home
        env['HADOOP_CLASSPATH'] = "{ex}:{home}/*:{libs}/*:{cp}".format(
            ex=giraph_examples,
            home=giraph_home,
            libs=giraph_libdir,
            cp=cur_cp
        )
        env['GIRAPH_JARS'] = ','.join(j for j in giraph_jars)

    set_state('giraph.installed')
    report_status()
    # set app version string for juju status output
    giraph_version = get_package_version('giraph') or 'unknown'
    hookenv.application_version_set(giraph_version)
