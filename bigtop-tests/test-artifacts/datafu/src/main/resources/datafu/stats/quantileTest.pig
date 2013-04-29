register $JAR_PATH

define Quantile datafu.pig.stats.Quantile($QUANTILES);

data_in = LOAD 'input' as (val:int);

/*describe data_in;*/

data_out = GROUP data_in ALL;

/*describe data_out;*/

data_out = FOREACH data_out {
  sorted = ORDER data_in BY val;
  GENERATE Quantile(sorted) as quantiles;
}
data_out = FOREACH data_out GENERATE FLATTEN(quantiles);

/*describe data_out;*/

STORE data_out into 'output';