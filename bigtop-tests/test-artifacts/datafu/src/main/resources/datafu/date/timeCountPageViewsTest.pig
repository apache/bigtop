register $JAR_PATH

define TimeCount datafu.pig.date.TimeCount('$TIME_WINDOW');

views = LOAD 'input' AS (user_id:int, page_id:int, time:chararray);

views_grouped = GROUP views BY (user_id, page_id);
view_counts = foreach views_grouped {
  views = order views by time;
  generate group.user_id as user_id, group.page_id as page_id, TimeCount(views.(time)) as count;
}

STORE view_counts INTO 'output';
