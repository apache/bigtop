#!/bin/bash
export PGPORT=<%= @master_port %>;<%= @gp_home %>/bin/pg_ctl -w -l $1/log/startup.log -D $1 -o "-p <%= @master_port %> --gp_dbid=1 -i --gp_contentid=-1 -c gp_role=utility -m" start
