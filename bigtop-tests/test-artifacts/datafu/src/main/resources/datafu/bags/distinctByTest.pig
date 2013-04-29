register $JAR_PATH

define DistinctBy datafu.pig.bags.DistinctBy('0');

data = LOAD 'input' AS (data: bag {T: tuple(a:CHARARRAY, b:INT, c:INT)});

data2 = FOREACH data GENERATE DistinctBy(data);

describe data2;

STORE data2 INTO 'output';

