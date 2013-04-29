register $JAR_PATH

define Median datafu.pig.stats.Median();

data_in = LOAD 'input' as (val:int);

/*describe data_in;*/

data_out = GROUP data_in ALL;

/*describe data_out;*/

data_out = FOREACH data_out {
  sorted = ORDER data_in BY val;
  GENERATE Median(sorted) as medians;
}
data_out = FOREACH data_out GENERATE FLATTEN(medians);

/*describe data_out;*/

STORE data_out into 'output';