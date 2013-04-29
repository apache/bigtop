register $JAR_PATH

define UserAgentClassify datafu.pig.urls.UserAgentClassify();

data = load 'input' as (usr_agent:chararray);
data_out = foreach data generate UserAgentClassify(usr_agent) as class;
describe data_out;
store data_out into 'output';