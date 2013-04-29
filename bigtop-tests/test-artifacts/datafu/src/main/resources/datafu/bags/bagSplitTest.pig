register $JAR_PATH

define BagSplit datafu.pig.bags.BagSplit();

data = LOAD 'input' AS (B:bag{T:tuple(val1:INT,val2:INT)});

data2 = FOREACH data GENERATE BagSplit($MAX,B);
describe data2;

data3 = FOREACH data2 GENERATE FLATTEN($0);

describe data3

STORE data3 INTO 'output';
