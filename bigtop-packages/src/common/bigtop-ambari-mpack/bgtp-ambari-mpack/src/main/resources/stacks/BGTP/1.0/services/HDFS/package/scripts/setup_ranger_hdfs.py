#!/usr/bin/env python
"""
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

"""
import os
from resource_management.core.logger import Logger
from resource_management.core.resources.system import Execute
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.constants import Direction
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions import setup_ranger_plugin_xml
from resource_management.libraries.functions import ranger_functions_v2

def setup_ranger_hdfs(upgrade_type=None):
  import params

  if params.enable_ranger_hdfs:

    stack_version = None

    if upgrade_type is not None:
      stack_version = params.version

    if params.retryAble:
      Logger.info("HDFS: Setup ranger: command retry enables thus retrying if ranger admin is down !")
    else:
      Logger.info("HDFS: Setup ranger: command retry not enabled thus skipping if ranger admin is down !")

    if params.is_hdfs_federation_enabled and params.is_namenode_host:
      if params.namenode_nameservice is not None and params.fs_default_name == format("hdfs://{namenode_nameservice}"):
        update_ranger_hdfs_service_name()

    api_version = 'v2'
    setup_ranger_plugin_xml.setup_ranger_plugin('hadoop-client', 'hdfs', params.previous_jdbc_jar,
                        params.downloaded_custom_connector, params.driver_curl_source,
                        params.driver_curl_target, params.java_home,
                        params.repo_name, params.hdfs_ranger_plugin_repo,
                        params.ranger_env, params.ranger_plugin_properties,
                        params.policy_user, params.policymgr_mgr_url,
                        params.enable_ranger_hdfs, conf_dict = params.hadoop_conf_dir,
                        component_user = params.hdfs_user, component_group = params.user_group, cache_service_list = ['hdfs'],
                        plugin_audit_properties = params.config['configurations']['ranger-hdfs-audit'], plugin_audit_attributes = params.config['configurationAttributes']['ranger-hdfs-audit'],
                        plugin_security_properties = params.config['configurations']['ranger-hdfs-security'], plugin_security_attributes = params.config['configurationAttributes']['ranger-hdfs-security'],
                        plugin_policymgr_ssl_properties = params.config['configurations']['ranger-hdfs-policymgr-ssl'], plugin_policymgr_ssl_attributes = params.config['configurationAttributes']['ranger-hdfs-policymgr-ssl'],
                        component_list = ['hadoop-client'], audit_db_is_enabled = params.xa_audit_db_is_enabled,
                        credential_file = params.credential_file, xa_audit_db_password = params.xa_audit_db_password,
                        ssl_truststore_password = params.ssl_truststore_password, ssl_keystore_password = params.ssl_keystore_password,
                        api_version = api_version ,stack_version_override = stack_version, skip_if_rangeradmin_down = not params.retryAble,
                        is_security_enabled = params.security_enabled,
                        is_stack_supports_ranger_kerberos = params.stack_supports_ranger_kerberos,
                        component_user_principal = params.nn_principal_name if params.security_enabled else None,
                        component_user_keytab = params.nn_keytab if params.security_enabled else None)
  else:
    Logger.info('Ranger Hdfs plugin is not enabled')

def create_ranger_audit_hdfs_directories():
  import params

  if params.enable_ranger_hdfs and params.xml_configurations_supported and params.xa_audit_hdfs_is_enabled:
    params.HdfsResource("/ranger/audit",
                       type="directory",
                       action="create_on_execute",
                       owner=params.hdfs_user,
                       group=params.hdfs_user,
                       mode=0755,
                       recursive_chmod=True,
    )
    params.HdfsResource("/ranger/audit/hdfs",
                       type="directory",
                       action="create_on_execute",
                       owner=params.hdfs_user,
                       group=params.hdfs_user,
                       mode=0700,
                       recursive_chmod=True,
    )
    params.HdfsResource(None, action="execute")
  else:
    Logger.info('Skipping creation of audit directory for Ranger Hdfs Plugin.')

def update_ranger_hdfs_service_name():
  """
  This is used for renaming and updating the default service created on Ranger Admin for NN Federation enabled cluster
  """
  import params

  service_name_exist = setup_ranger_plugin_xml.get_policycache_service_name(service_name = "hdfs", repo_name = params.repo_name, cache_service_list = ['hdfs'])

  if not service_name_exist:

    get_repo_name = None
    ranger_admin_v2_obj = ranger_functions_v2.RangeradminV2(url = params.policymgr_mgr_url, skip_if_rangeradmin_down = not params.retryAble)

    user_create_response = ranger_admin_v2_obj.create_ambari_admin_user(ambari_admin_username = params.ranger_env['ranger_admin_username'], ambari_admin_password = params.ranger_env['ranger_admin_password'], usernamepassword = params.ranger_env['admin_username'] + ":" + params.ranger_env['admin_password'])
    if user_create_response is not None and user_create_response == 200:
      get_repo_name = ranger_admin_v2_obj.get_repository_by_name_urllib2(name = params.repo_name_default, component = "hdfs", status = "true", usernamepassword = params.ranger_env['ranger_admin_username'] + ":" + params.ranger_env['ranger_admin_password'])

    if get_repo_name is not None and get_repo_name['name'] == params.repo_name_default:
      update_repo_name = ranger_admin_v2_obj.update_repository_urllib2(component = "hdfs", repo_name = params.repo_name_default, repo_properties = params.hdfs_ranger_plugin_repo,
        admin_user = params.ranger_env['ranger_admin_username'], admin_password = params.ranger_env['ranger_admin_password'], force_rename = True)