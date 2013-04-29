register $JAR_PATH

define IntToBool datafu.pig.util.IntToBool();

data = LOAD 'input' AS (val:INT);

data2 = FOREACH data GENERATE IntToBool(val);

STORE data2 INTO 'output';

