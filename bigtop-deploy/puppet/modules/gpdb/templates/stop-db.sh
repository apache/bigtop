#!/bin/bash
<%= @gp_home %>/bin/pg_ctl -p $2 -D $1 stop
