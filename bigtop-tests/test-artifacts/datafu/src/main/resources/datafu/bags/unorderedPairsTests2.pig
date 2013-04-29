register $JAR_PATH

define UnorderedPairs datafu.pig.bags.UnorderedPairs();

data = LOAD 'input' AS (A:int, B: bag {T: tuple(v:INT)});

data2 = FOREACH data GENERATE A, UnorderedPairs(B) as P;

data3 = FOREACH data2 GENERATE A, FLATTEN(P);

STORE data3 INTO 'output';

