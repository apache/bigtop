#!/bin/bash
if [ -f /etc/os-release ]; then
    . /etc/os-release
fi
OS="$ID"
if [ "${OS}" = "fedora" ]; then
    export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/usr/lib/gpdb/lib;
fi

export PGPORT=<%= @master_port %>;<%= @gp_home %>/bin/pg_ctl -w -l $1/pg_log/startup.log -D $1 -o "-p <%= @master_port %> --gp_dbid=1 -i --gp_contentid=-1 -c gp_role=utility -m" start
