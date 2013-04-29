register $JAR_PATH

define Enumerate datafu.pig.bags.Enumerate('1');

data = LOAD 'input' AS (data: bag {T: tuple(v1:INT,B: bag{T: tuple(v2:INT)})});

data2 = FOREACH data GENERATE Enumerate(data);
describe data2;

data3 = FOREACH data2 GENERATE FLATTEN($0);
describe data3;

data4 = FOREACH data3 GENERATE $0 as v1, $1 as B, $2 as i;
describe data4;

STORE data4 INTO 'output';
