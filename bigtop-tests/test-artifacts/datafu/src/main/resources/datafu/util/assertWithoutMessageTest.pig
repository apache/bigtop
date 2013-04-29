register $JAR_PATH

define ASSERT datafu.pig.util.ASSERT();

data = LOAD 'input' AS (val:INT);

data2 = FILTER data BY ASSERT(val);

STORE data2 INTO 'output';

