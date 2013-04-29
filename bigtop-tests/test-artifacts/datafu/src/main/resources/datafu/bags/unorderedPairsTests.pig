register $JAR_PATH

define UnorderedPairs datafu.pig.bags.UnorderedPairs();

data = LOAD 'input' AS (B: bag {T: tuple(v:INT)});

data2 = FOREACH data GENERATE UnorderedPairs(B) as P;

data3 = FOREACH data2 GENERATE FLATTEN(P);

data4 = FOREACH data3 GENERATE FLATTEN(elem1), FLATTEN(elem2);

data5 = ORDER data4 BY $0, $1;

STORE data5 INTO 'output';

