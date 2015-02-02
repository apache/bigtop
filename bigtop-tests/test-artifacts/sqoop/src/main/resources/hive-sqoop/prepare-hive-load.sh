#!/usr/bin/bash

outpath=$(cat output_directory.txt)

testing=$(hadoop fs -ls $outpath/test_table | grep 'part' | awk -F' ' '{print $NF}');

echo "drop table if exists test_hive;" > hiveTransfer.sql
echo "create table test_hive (a int, b string) row format delimited fields terminated by ',';" >> hiveTransfer.sql

for inputPath in $testing
do

echo "load data inpath '$inputPath' into table test_hive;" >> hiveTransfer.sql 

done

