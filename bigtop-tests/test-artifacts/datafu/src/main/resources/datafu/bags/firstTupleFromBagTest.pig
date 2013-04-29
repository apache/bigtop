register $JAR_PATH

define FirstTupleFromBag datafu.pig.bags.FirstTupleFromBag();

data = LOAD 'input' AS (key:INT, B: bag{T: tuple(v:INT)});

data2 = FOREACH data GENERATE key, FirstTupleFromBag(B, null) as B;

STORE data2 INTO 'output';
