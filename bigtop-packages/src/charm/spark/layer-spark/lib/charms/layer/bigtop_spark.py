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
import time
from jujubigdata import utils
from path import Path

from charms.layer.apache_bigtop_base import Bigtop
from charms import layer
from charmhelpers.core import hookenv, host, unitdata
from charmhelpers.fetch.archiveurl import ArchiveUrlFetchHandler
from charmhelpers.payload import archive


class Spark(object):
    """
    This class manages Spark.
    """
    def __init__(self):
        self.dist_config = utils.DistConfig(
            data=layer.options('hadoop-client'))

    # translate our execution_mode into the appropriate --master value
    def get_master_url(self, spark_master_host):
        mode = hookenv.config()['spark_execution_mode']
        zk_units = unitdata.kv().get('zookeeper.units', [])
        master = None
        if mode.startswith('local') or mode.startswith('yarn'):
            master = mode
        elif mode == 'standalone' and not zk_units:
            master = 'spark://{}:7077'.format(spark_master_host)
        elif mode == 'standalone' and zk_units:
            master_ips = [p[1] for p in unitdata.kv().get('sparkpeer.units')]
            nodes = []
            for ip in master_ips:
                nodes.append('{}:7077'.format(ip))
            nodes_str = ','.join(nodes)
            master = 'spark://{}'.format(nodes_str)
        return master

    def configure_sparkbench(self):
        """
        Install/configure/remove Spark-Bench based on user config.

        If config[spark_bench_enabled], fetch, install, and configure
        Spark-Bench on initial invocation. Subsequent invocations will skip the
        fetch/install, but will reconfigure Spark-Bench since we may need to
        adjust the data dir (eg: benchmark data is stored in hdfs when spark
        is in yarn mode; locally in all other execution modes).
        """
        install_sb = hookenv.config()['spark_bench_enabled']
        sb_dir = '/home/ubuntu/SparkBench'
        if install_sb:
            # Fetch/install on our first go-round, then set unit data so we
            # don't reinstall every time this function is called.
            if not unitdata.kv().get('spark_bench.installed', False):
                sb_url = hookenv.config()['spark_bench_url']

                Path(sb_dir).rmtree_p()
                au = ArchiveUrlFetchHandler()
                au.install(sb_url, '/home/ubuntu')

                # NB: This block is unused when using one of our sb tgzs. It
                # may come in handy if people want a tgz that does not expand
                # to our expected sb_dir.
                # #####
                # Handle glob if we use a .tgz that doesn't expand to sb_dir
                # sb_archive_dir = glob('/home/ubuntu/SparkBench*')[0]
                # SparkBench expects to live in ~/SparkBench, so put it there
                # Path(sb_archive_dir).rename(sb_dir)
                # #####

                # Ensure users in the spark group can write to any subdirectory
                # of sb_dir (spark needs to write benchmark output there when
                # running in local modes).
                host.chownr(Path(sb_dir), 'ubuntu', 'spark', chowntopdir=True)
                for r, d, f in os.walk(sb_dir):
                    os.chmod(r, 0o2775)

                unitdata.kv().set('spark_bench.installed', True)
                unitdata.kv().flush(True)

            # Configure the SB env every time this function is called.
            sb_conf = '{}/conf'.format(sb_dir)
            sb_env = Path(sb_conf) / 'env.sh'
            if not sb_env.exists():
                (Path(sb_conf) / 'env.sh.template').copy(sb_env)

            # NB: A few notes on configuring SparkBench:
            # 1. Input data has been pregenerated and packed into the tgz. All
            # spark cluster members will have this data locally, which enables
            # us to execute benchmarks in the absense of HDFS. When spark is in
            # yarn mode, we'll need to generate and store this data in HDFS
            # so nodemanagers can access it (NMs obviously won't have SB
            # installed locally). Set DATA_HDFS to a local dir or common HDFS
            # location depending on our spark execution mode.
            #
            # 2. SB tries to SSH to spark workers to purge vmem caches. This
            # isn't possible in containers, nor is it possible in our env
            # because we don't distribute ssh keys among cluster members.
            # Set MC_LIST to an empty string to prevent this behavior.
            #
            # 3. Throughout SB, HADOOP_HOME/bin is used as the prefix for the
            # hdfs command. Bigtop's hdfs lives at /usr/bin/hdfs, so set the
            # SB HADOOP_HOME accordingly (it's not used for anything else).
            #
            # 4. Use our MASTER envar to set the SparkBench SPARK_MASTER url.
            # It is updated every time we (re)configure spark.
            mode = hookenv.config()['spark_execution_mode']
            if mode.startswith('yarn'):
                sb_data_dir = "hdfs:///user/ubuntu/SparkBench"
            else:
                sb_data_dir = "file://{}".format(sb_dir)

            utils.re_edit_in_place(sb_env, {
                r'^DATA_HDFS *=.*': 'DATA_HDFS="{}"'.format(sb_data_dir),
                r'^DATASET_DIR *=.*': 'DATASET_DIR="{}/dataset"'.format(sb_dir),
                r'^MC_LIST *=.*': 'MC_LIST=""',
                r'.*HADOOP_HOME *=.*': 'HADOOP_HOME="/usr"',
                r'.*SPARK_HOME *=.*': 'SPARK_HOME="/usr/lib/spark"',
                r'^SPARK_MASTER *=.*': 'SPARK_MASTER="$MASTER"',
            })
        else:
            # config[spark_bench_enabled] is false; remove it
            Path(sb_dir).rmtree_p()
            unitdata.kv().set('spark_bench.installed', False)
            unitdata.kv().flush(True)

    def configure_examples(self):
        """
        Install sparkpi.sh and sample data to /home/ubuntu.

        The sparkpi.sh script demonstrates spark-submit with the SparkPi class
        included with Spark. This small script is packed into the spark charm
        source in the ./scripts subdirectory.

        The sample data is used for benchmarks (only PageRank for now). This
        may grow quite large in the future, so we utilize Juju Resources for
        getting this data onto the unit. Sample data originated as follows:

        - PageRank: https://snap.stanford.edu/data/web-Google.html
        """
        # Handle sparkpi.sh
        script_source = 'scripts/sparkpi.sh'
        script_path = Path(script_source)
        if script_path.exists():
            script_target = '/home/ubuntu/sparkpi.sh'
            new_hash = host.file_hash(script_source)
            old_hash = unitdata.kv().get('sparkpi.hash')
            if new_hash != old_hash:
                hookenv.log('Installing SparkPi script')
                script_path.copy(script_target)
                Path(script_target).chmod(0o755)
                Path(script_target).chown('ubuntu', 'hadoop')
                unitdata.kv().set('sparkpi.hash', new_hash)
                hookenv.log('SparkPi script was installed successfully')

        # Handle sample data
        sample_source = hookenv.resource_get('sample-data')
        sample_path = sample_source and Path(sample_source)
        if sample_path and sample_path.exists() and sample_path.stat().st_size:
            sample_target = '/home/ubuntu'
            new_hash = host.file_hash(sample_source)
            old_hash = unitdata.kv().get('sample-data.hash')
            if new_hash != old_hash:
                hookenv.log('Extracting Spark sample data')
                # Extract the sample data; since sample data does not impact
                # functionality, log any extraction error but don't fail.
                try:
                    archive.extract(sample_path, destpath=sample_target)
                except Exception:
                    hookenv.log('Unable to extract Spark sample data: {}'
                                .format(sample_path))
                else:
                    unitdata.kv().set('sample-data.hash', new_hash)
                    hookenv.log('Spark sample data was extracted successfully')

    def configure_events_dir(self, mode):
        """
        Create directory for spark event data.

        This directory is used by workers to store event data. It is also read
        by the history server when displaying event information.

        :param string mode: Spark execution mode to determine the dir location.
        """
        dc = self.dist_config

        # Directory needs to be 777 so non-spark users can write job history
        # there. It needs to be g+s (HDFS is g+s by default) so all entries
        # are readable by spark (in the spark group). It needs to be +t so
        # users cannot remove files they don't own.
        if mode.startswith('yarn'):
            events_dir = 'hdfs://{}'.format(dc.path('spark_events'))
            utils.run_as('hdfs', 'hdfs', 'dfs', '-mkdir', '-p', events_dir)
            utils.run_as('hdfs', 'hdfs', 'dfs', '-chmod', '1777', events_dir)
            utils.run_as('hdfs', 'hdfs', 'dfs', '-chown', '-R', 'ubuntu:spark',
                         events_dir)
        else:
            events_dir = dc.path('spark_events')
            events_dir.makedirs_p()
            events_dir.chmod(0o3777)
            host.chownr(events_dir, 'ubuntu', 'spark', chowntopdir=True)

    def configure(self, available_hosts, zk_units, peers, extra_libs):
        """
        This is the core logic of setting up spark.

        :param dict available_hosts: Hosts that Spark should know about.
        :param list zk_units: List of Zookeeper dicts with host/port info.
        :param list peers: List of Spark peer tuples (unit name, IP).
        :param list extra_libs: List of extra lib paths for driver/executors.
        """
        # Set KV based on connected applications
        unitdata.kv().set('zookeeper.units', zk_units)
        unitdata.kv().set('sparkpeer.units', peers)
        unitdata.kv().flush(True)

        # Get our config ready
        dc = self.dist_config
        mode = hookenv.config()['spark_execution_mode']
        master_ip = utils.resolve_private_address(available_hosts['spark-master'])
        master_url = self.get_master_url(master_ip)
        req_driver_mem = hookenv.config()['driver_memory']
        req_executor_mem = hookenv.config()['executor_memory']
        if mode.startswith('yarn'):
            spark_events = 'hdfs://{}'.format(dc.path('spark_events'))
        else:
            spark_events = 'file://{}'.format(dc.path('spark_events'))

        # handle tuning options that may be set as percentages
        driver_mem = '1g'
        executor_mem = '1g'
        if req_driver_mem.endswith('%'):
            if mode == 'standalone' or mode.startswith('local'):
                mem_mb = host.get_total_ram() / 1024 / 1024
                req_percentage = float(req_driver_mem.strip('%')) / 100
                driver_mem = str(int(mem_mb * req_percentage)) + 'm'
            else:
                hookenv.log("driver_memory percentage in non-local mode. "
                            "Using 1g default.", level=hookenv.WARNING)
        else:
            driver_mem = req_driver_mem

        if req_executor_mem.endswith('%'):
            if mode == 'standalone' or mode.startswith('local'):
                mem_mb = host.get_total_ram() / 1024 / 1024
                req_percentage = float(req_executor_mem.strip('%')) / 100
                executor_mem = str(int(mem_mb * req_percentage)) + 'm'
            else:
                hookenv.log("executor_memory percentage in non-local mode. "
                            "Using 1g default.", level=hookenv.WARNING)
        else:
            executor_mem = req_executor_mem

        # Some spark applications look for envars in /etc/environment
        with utils.environment_edit_in_place('/etc/environment') as env:
            env['MASTER'] = master_url
            env['SPARK_HOME'] = dc.path('spark_home')

        # Setup hosts dict
        hosts = {
            'spark': master_ip,
        }
        if 'namenode' in available_hosts:
            hosts['namenode'] = available_hosts['namenode']
        if 'resourcemanager' in available_hosts:
            hosts['resourcemanager'] = available_hosts['resourcemanager']

        # Setup roles dict. We always include the history server and client.
        # Determine other roles based on our execution mode.
        roles = ['spark-history-server', 'spark-client']
        if mode == 'standalone':
            roles.append('spark-master')
            roles.append('spark-worker')
        elif mode.startswith('yarn'):
            roles.append('spark-on-yarn')
            roles.append('spark-yarn-slave')

        # Setup overrides dict
        override = {
            'spark::common::master_url': master_url,
            'spark::common::event_log_dir': spark_events,
            'spark::common::history_log_dir': spark_events,
            'spark::common::extra_lib_dirs':
                ':'.join(extra_libs) if extra_libs else None,
            'spark::common::driver_mem': driver_mem,
            'spark::common::executor_mem': executor_mem,
        }
        if zk_units:
            zks = []
            for unit in zk_units:
                ip = utils.resolve_private_address(unit['host'])
                zks.append("%s:%s" % (ip, unit['port']))

            zk_connect = ",".join(zks)
            override['spark::common::zookeeper_connection_string'] = zk_connect
        else:
            override['spark::common::zookeeper_connection_string'] = None

        # Create our site.yaml and trigger puppet.
        # NB: during an upgrade, we configure the site.yaml, but do not
        # trigger puppet. The user must do that with the 'reinstall' action.
        bigtop = Bigtop()
        bigtop.render_site_yaml(hosts, roles, override)
        if unitdata.kv().get('spark.version.repo', False):
            hookenv.log("An upgrade is available and the site.yaml has been "
                        "configured. Run the 'reinstall' action to continue.",
                        level=hookenv.INFO)
        else:
            bigtop.trigger_puppet()
            self.patch_worker_master_url(master_ip, master_url)

            # Packages don't create the event dir by default. Do it each time
            # spark is (re)installed to ensure location/perms are correct.
            self.configure_events_dir(mode)

        # Handle examples and Spark-Bench. Do this each time this method is
        # called in case we need to act on a new resource or user config.
        self.configure_examples()
        self.configure_sparkbench()

    def patch_worker_master_url(self, master_ip, master_url):
        """
        Patch the worker startup script to use the full master url istead of contracting it.
        The master url is placed in the spark-env.sh so that the startup script will use it.
        In HA mode the master_ip is set to be the local_ip instead of the one the leader
        elects. This requires a restart of the master service.
        """
        zk_units = unitdata.kv().get('zookeeper.units', [])
        if master_url.startswith('spark://'):
            if zk_units:
                master_ip = hookenv.unit_private_ip()
            spark_env = '/etc/spark/conf/spark-env.sh'
            utils.re_edit_in_place(spark_env, {
                r'.*SPARK_MASTER_URL.*': "export SPARK_MASTER_URL={}".format(master_url),
                r'.*SPARK_MASTER_IP.*': "export SPARK_MASTER_IP={}".format(master_ip),
            }, append_non_matches=True)

            self.inplace_change('/etc/init.d/spark-worker',
                                'spark://$SPARK_MASTER_IP:$SPARK_MASTER_PORT',
                                '$SPARK_MASTER_URL')

    def inplace_change(self, filename, old_string, new_string):
        # Safely read the input filename using 'with'
        with open(filename) as f:
            s = f.read()
            if old_string not in s:
                return

        # Safely write the changed content, if found in the file
        with open(filename, 'w') as f:
            s = s.replace(old_string, new_string)
            f.write(s)

    def start(self):
        """
        Always start the Spark History Server. Start other services as
        required by our execution mode. Open related ports as appropriate.
        """
        host.service_start('spark-history-server')
        hookenv.open_port(self.dist_config.port('spark-history-ui'))

        # Spark master/worker is only started in standalone mode
        if hookenv.config()['spark_execution_mode'] == 'standalone':
            if host.service_start('spark-master'):
                hookenv.log("Spark Master started")
                hookenv.open_port(self.dist_config.port('spark-master-ui'))
                # If the master started and we have peers, wait 2m for recovery
                # before starting the worker. This ensures the worker binds
                # to the correct master.
                if unitdata.kv().get('sparkpeer.units'):
                    hookenv.status_set('maintenance',
                                       'waiting for spark master recovery')
                    hookenv.log("Waiting 2m to ensure spark master is ALIVE")
                    time.sleep(120)
            else:
                hookenv.log("Spark Master did not start; this is normal "
                            "for non-leader units in standalone mode")

            # NB: Start the worker even if the master process on this unit
            # fails to start. In non-HA mode, spark master only runs on the
            # leader. On non-leader units, we still want a worker bound to
            # the leader.
            if host.service_start('spark-worker'):
                hookenv.log("Spark Worker started")
                hookenv.open_port(self.dist_config.port('spark-worker-ui'))
            else:
                hookenv.log("Spark Worker did not start")

    def stop(self):
        """
        Stop all services (and close associated ports). Stopping a service
        that is not currently running does no harm.
        """
        host.service_stop('spark-history-server')
        hookenv.close_port(self.dist_config.port('spark-history-ui'))

        # Stop the worker before the master
        host.service_stop('spark-worker')
        hookenv.close_port(self.dist_config.port('spark-worker-ui'))
        host.service_stop('spark-master')
        hookenv.close_port(self.dist_config.port('spark-master-ui'))
