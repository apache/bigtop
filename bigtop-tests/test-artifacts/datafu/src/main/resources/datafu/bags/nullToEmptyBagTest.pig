register $JAR_PATH

define NullToEmptyBag datafu.pig.bags.NullToEmptyBag();

data = LOAD 'input' AS (B: bag {T: tuple(v:INT)});

dump data;

data2 = FOREACH data GENERATE NullToEmptyBag(B) as P;

dump data2;

STORE data2 INTO 'output';

