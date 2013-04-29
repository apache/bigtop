register $JAR_PATH

define RandInt datafu.pig.numbers.RandInt();

data = LOAD 'input' AS (key:INT);
data2 = FOREACH data GENERATE key, RandInt($MIN,$MAX) as val;

STORE data2 INTO 'output';
