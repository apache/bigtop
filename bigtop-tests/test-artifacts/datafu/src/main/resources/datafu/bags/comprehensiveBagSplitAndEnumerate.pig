register $JAR_PATH

define BagSplit datafu.pig.bags.BagSplit();
define Enumerate datafu.pig.bags.Enumerate('1');

data = LOAD 'input' AS (data: bag {T: tuple(name:CHARARRAY, score:double)});

data2 = FOREACH data GENERATE BagSplit(3,data) as the_bags;

describe data2

data3 = FOREACH data2 GENERATE Enumerate(the_bags) as enumerated_bags;

describe data3

data4 = FOREACH data3 GENERATE FLATTEN(enumerated_bags) as (data,i);

describe data4

data5 = FOREACH data4 GENERATE data as the_data, i as the_key;

describe data5

data_out = FOREACH data5 GENERATE FLATTEN(the_data), the_key;

describe data_out