#!/bin/sh
<%= @gp_home %>/bin/psql -p <%= @master_port %> -d "template1" -c "insert into gp_fault_strategy(fault_strategy) values ('n');" || true
