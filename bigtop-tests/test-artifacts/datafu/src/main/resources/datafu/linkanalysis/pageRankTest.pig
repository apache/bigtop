register $JAR_PATH

/* Need to enable dangling node handling since the Wikipedia example has them,
   otherwise the ranks won't be right. */
define PageRank datafu.pig.linkanalysis.PageRank('dangling_nodes','true');

data = LOAD 'input' AS (topic:INT,source:INT,dest:INT,weight:DOUBLE);

data_grouped = GROUP data by (topic,source);

data_grouped = foreach data_grouped {
  generate group.topic as topic, group.source as source, data.(dest,weight) as edges;
};

data_grouped2 = GROUP data_grouped by topic;
data_grouped2 = foreach data_grouped2 {
  generate group as topic, FLATTEN(PageRank(data_grouped.(source,edges))) as (source,rank);
};

data_grouped3 = FOREACH data_grouped2 GENERATE
  topic,
  source,
  rank;
  
STORE data_grouped3 INTO 'output';
