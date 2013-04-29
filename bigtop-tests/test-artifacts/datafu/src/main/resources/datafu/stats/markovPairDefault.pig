register $JAR_PATH

define markovPairs datafu.pig.stats.MarkovPairs();

data = load 'input' as $schema;
describe data;

data_out1 = foreach data generate data as orig_bag;
describe data_out1;

data_out = foreach data_out1 generate markovPairs(orig_bag) as markov_bag;
describe data_out;

store data_out into 'output';