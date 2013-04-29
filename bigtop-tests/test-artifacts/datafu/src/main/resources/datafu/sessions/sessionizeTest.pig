register $JAR_PATH

define Sessionize datafu.pig.sessions.Sessionize('$TIME_WINDOW');

views = LOAD 'input' AS (time:chararray, user_id:int, value:int);

views_grouped = GROUP views BY user_id;
view_counts = FOREACH views_grouped {
  views = ORDER views BY time;
  GENERATE flatten(Sessionize(views)) as (time,user_id,value,session_id);
}

max_value = GROUP view_counts BY (user_id, session_id);

max_value = FOREACH max_value GENERATE group.user_id, MAX(view_counts.value) AS val;

STORE max_value INTO 'output';
