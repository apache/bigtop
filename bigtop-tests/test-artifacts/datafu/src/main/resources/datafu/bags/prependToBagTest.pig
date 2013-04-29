register $JAR_PATH

define PrependToBag datafu.pig.bags.PrependToBag();

data = LOAD 'input' AS (key:INT, B: bag{T: tuple(v:INT)}, T: tuple(v:INT));

data2 = FOREACH data GENERATE key, PrependToBag(B,T) as B;

STORE data2 INTO 'output';
