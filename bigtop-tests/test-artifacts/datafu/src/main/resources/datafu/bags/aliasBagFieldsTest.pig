register $JAR_PATH

define AliasBagFields datafu.pig.bags.AliasBagFields('[a#alpha,b#numeric]');

data = LOAD 'input' AS (data: bag {T: tuple(a:CHARARRAY, b:INT, c:INT)});

data2 = FOREACH data GENERATE AliasBagFields(data) as data;

describe data2;

data3 = FOREACH data2 GENERATE FLATTEN(data);

describe data3;

data4 = FOREACH data3 GENERATE data::alpha, data::numeric;

describe data4;

STORE data4 INTO 'output';

