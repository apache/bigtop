register $JAR_PATH

define BagSplit datafu.pig.bags.BagSplit('true');

data = LOAD 'input' AS (B:bag{T:tuple(val1:INT,val2:INT)});

data2 = FOREACH data GENERATE BagSplit($MAX,B);

data3 = FOREACH data2 GENERATE FLATTEN($0);

STORE data3 INTO 'output';