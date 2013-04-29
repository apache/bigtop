register $JAR_PATH

define MD5 datafu.pig.hash.MD5();

data_in = LOAD 'input' as (val:chararray);

data_out = FOREACH data_in GENERATE MD5(val) as val;

STORE data_out INTO 'output';