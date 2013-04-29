register $JAR_PATH

define WilsonBinConf datafu.pig.stats.WilsonBinConf('$alpha');

data = load 'input' as (successes:long, totals:long);
describe data;

data_out = FOREACH data GENERATE WilsonBinConf(successes, totals) as interval;
data_out = FOREACH data_out GENERATE FLATTEN(interval);

store data_out into 'output';