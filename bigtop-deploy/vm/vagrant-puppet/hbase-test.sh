#!/bin/bash

vagrant ssh -c "hbase shell <<EOF
create 't1','cf1'
put 't1', 'row1', 'cf1:q1', 'value1'
scan 't1'
disable 't1'
drop 't1'
EOF"
