from charms.reactive import when, when_not
from charms.reactive import is_state, set_state, remove_state
from charmhelpers.core import hookenv
from charms.layer.apache_bigtop_base import get_layer_opts
from charms.layer.bigtop_hive import Hive


@when('bigtop.available')
def report_status():
    hadoop_joined = is_state('hadoop.joined')
    hadoop_ready = is_state('hadoop.ready')
    database_joined = is_state('database.connected')
    database_ready = is_state('database.available')
    hive_installed = is_state('hive.installed')
    if not hadoop_joined:
        hookenv.status_set('blocked',
                           'waiting for relation to hadoop plugin')
    elif not hadoop_ready:
        hookenv.status_set('waiting',
                           'waiting for hadoop')
    elif database_joined and not database_ready:
        hookenv.status_set('waiting',
                           'waiting for database')
    elif hive_installed and not database_ready:
        hookenv.status_set('active',
                           'ready (local db, hiverserver2 unavailable)')
    elif hive_installed and database_ready:
        hookenv.status_set('active',
                           'ready')


@when('bigtop.available', 'hadoop.ready')
@when_not('hive.installed')
def install_hive(hadoop):
    # Hive cannot handle - in the metastore db name and
    # mysql uses the service name to name the db
    if "-" in hookenv.service_name():
        hookenv.status_set('blocked', 'Service name should not contain -. '
                                      'Redeploy with a different name.')
        return

    hive = Hive()
    hookenv.status_set('maintenance', 'installing hive')
    hive.install()
    hive.initial_hive_config()
    set_state('hive.installed')
    hookenv.status_set('active', 'ready (local db, hiveserver2 unavailable)')


@when('hive.installed', 'config.changed.heap')
def config_changed():
    hookenv.status_set('maintenance', 'configuring with new options')
    hive = Hive()
    hive.configure_hive()
    if is_state('hive.db.configured'):
        # Only restart hiveserver2 if we have an external db configured
        hive.restart()
    hookenv.status_set('active', 'ready')


@when('hive.installed', 'database.available')
@when_not('hive.db.configured')
def configure_with_remote_db(database):
    hookenv.status_set('maintenance', 'configuring external database; starting hiveserver2')
    hive = Hive()
    hive.configure_remote_db(database)
    hive.start()
    hive.open_ports()
    set_state('hive.db.configured')
    hookenv.status_set('active', 'ready')


@when('hive.installed', 'hive.db.configured')
@when_not('database.available')
def configure_with_local_db():
    hookenv.status_set('maintenance', 'configuring local database; stopping hiveserver2')
    hive = Hive()
    hive.stop()
    hive.close_ports()
    hive.configure_local_db()
    remove_state('hive.db.configured')
    hookenv.status_set('active', 'ready (local db, hiveserver2 unavailable)')


@when('hive.installed')
@when_not('hadoop.ready')
def stop_hive():
    hive = Hive()
    hive.stop()
    hive.close_ports()
    remove_state('hive.installed')


@when('hive.installed', 'hive.db.configured', 'client.joined')
def client_joined(client):
    # The client relation is all about access to HiveServer2, so we should only
    # send data if we have a client *and* we have a db configured. Having an
    # external db configured is a prerequisite condition for starting HiveServer2.
    port = get_layer_opts().port('hive')
    client.send_port(port)
    client.set_ready()
