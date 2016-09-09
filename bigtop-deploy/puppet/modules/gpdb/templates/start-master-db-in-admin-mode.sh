#!/bin/bash
export PGPORT=<%= master_port%>;<%= gp_home%>/bin/pg_ctl -w -l $1/pg_log/startup.log -D $1 -o "-i -p <%= master_port%> -c gp_role=utility -M master -b 1 -C -1 -z 0 -m" start