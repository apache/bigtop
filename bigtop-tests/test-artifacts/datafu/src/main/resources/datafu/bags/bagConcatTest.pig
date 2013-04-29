register $JAR_PATH

define BagConcat datafu.pig.bags.BagConcat();

data = LOAD 'input' AS (A: bag{T: tuple(v:INT)}, B: bag{T: tuple(v:INT)}, C: bag{T: tuple(v:INT)});

data2 = FOREACH data GENERATE BagConcat(A,B,C);

describe data2

STORE data2 INTO 'output';