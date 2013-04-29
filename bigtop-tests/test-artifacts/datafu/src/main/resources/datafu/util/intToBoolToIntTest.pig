register $JAR_PATH

define IntToBool datafu.pig.util.IntToBool();
define BoolToInt datafu.pig.util.BoolToInt();

data = LOAD 'input' AS (val:INT);

data2 = FOREACH data GENERATE IntToBool(val) as val;
data3 = FOREACH data2 GENERATE BoolToInt(val) as val;

STORE data3 INTO 'output';

