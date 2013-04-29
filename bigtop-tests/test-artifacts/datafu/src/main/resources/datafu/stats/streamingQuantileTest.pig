register $JAR_PATH

define Quantile datafu.pig.stats.StreamingQuantile($QUANTILES);

data_in = LOAD 'input' as (val:int);

/*describe data_in;*/

data_out = GROUP data_in ALL;

/*describe data_out;*/

data_out = FOREACH data_out GENERATE Quantile(data_in.val) as quantiles;
data_out = FOREACH data_out GENERATE FLATTEN(quantiles);

/*describe data_out;*/

STORE data_out into 'output';
