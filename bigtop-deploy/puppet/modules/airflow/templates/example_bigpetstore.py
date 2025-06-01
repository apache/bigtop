#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
import pendulum

from airflow.models import DAG
from airflow.models.param import Param
from airflow.operators.bash import BashOperator
from airflow.providers.apache.spark.operators.spark_submit import SparkSubmitOperator

with DAG(
    "bigpetstore_dag",
    params={"bigpetstore_jar_path": Param("bigpetstore-spark-3.5.0-all.jar")},
    schedule=None,
    start_date=pendulum.datetime(2021, 1, 1, tz="UTC"),
) as dag:
    clean_hdfs_task = BashOperator(
        bash_command="hdfs dfs -rm -f -r generated_data transformed_data",
        task_id="clean_hdfs_task",
    )

    clean_fs_task = BashOperator(
        bash_command="rm -f /tmp/PetStoreStats.json /tmp/recommendations.json",
        task_id="clean_fs_task",
    )

    generate_task = SparkSubmitOperator(
        application="{{ params.bigpetstore_jar_path }}",
        application_args=["generated_data", "10", "1000", "365", "345"],
        java_class="org.apache.bigtop.bigpetstore.spark.generator.SparkDriver",
        task_id="generate_task"
    )

    transform_task = SparkSubmitOperator(
        application="{{ params.bigpetstore_jar_path }}",
        application_args=["generated_data", "transformed_data"],
        java_class="org.apache.bigtop.bigpetstore.spark.etl.SparkETL",
        task_id="transform_task"
    )

    analyze_task = SparkSubmitOperator(
        application="{{ params.bigpetstore_jar_path }}",
        application_args=["transformed_data", "/tmp/PetStoreStats.json"],
        java_class="org.apache.bigtop.bigpetstore.spark.analytics.PetStoreStatistics",
        task_id="analyze_task"
    )

    recommend_task = SparkSubmitOperator(
        application="{{ params.bigpetstore_jar_path }}",
        application_args=["transformed_data", "/tmp/recommendations.json"],
        java_class="org.apache.bigtop.bigpetstore.spark.analytics.RecommendProducts",
        task_id="recommend_task"
    )

    [clean_hdfs_task, clean_fs_task] >> generate_task >> transform_task >> [analyze_task, recommend_task]

if __name__ == "__main__":
    dag.test()
