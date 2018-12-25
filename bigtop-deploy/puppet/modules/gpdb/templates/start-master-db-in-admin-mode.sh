#!/bin/bash
export PGPORT=<%= @master_port %>; <%= @gp_home %>/bin/pg_ctl -w -l $1/pg_log/startup.log -D $1 -o "-p <%= @master_port %> --gp_dbid=1 --gp_num_contents_in_cluster=0 --silent-mode=true -i -M master --gp_contentid=-1 -x 0 -c gp_role=utility -m" start
