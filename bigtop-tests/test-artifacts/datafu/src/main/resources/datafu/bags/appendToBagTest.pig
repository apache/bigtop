register $JAR_PATH

define AppendToBag datafu.pig.bags.AppendToBag();

data = LOAD 'input' AS (key:INT, B: bag{T: tuple(v:INT)}, T: tuple(v:INT));

data2 = FOREACH data GENERATE key, AppendToBag(B,T) as B;

STORE data2 INTO 'output';
