drop table if exists test_hive;

create table test_hive (a int, b string) row format delimited fields terminated by ',';

load data inpath '/tmp/hive-output-dir/test_table/part-m-00000' into table test_hive;
load data inpath '/tmp/hive-output-dir/test_table/part-m-00001' into table test_hive;
load data inpath '/tmp/hive-output-dir/test_table/part-m-00002' into table test_hive;
load data inpath '/tmp/hive-output-dir/test_table/part-m-00003' into table test_hive;
load data inpath '/tmp/hive-output-dir/test_table/part-m-00004' into table test_hive;
load data inpath '/tmp/hive-output-dir/test_table/part-m-00005' into table test_hive;
load data inpath '/tmp/hive-output-dir/test_table/part-m-00006' into table test_hive;
load data inpath '/tmp/hive-output-dir/test_table/part-m-00007' into table test_hive;
load data inpath '/tmp/hive-output-dir/test_table/part-m-00008' into table test_hive;
load data inpath '/tmp/hive-output-dir/test_table/part-m-00009' into table test_hive;

