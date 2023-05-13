#!/bin/sh
export PGOPTIONS="-c gp_session_role=utility";<%= @gp_home %>/bin/psql -p <%= @master_port %> -d "template1" -c "select age from bigtopusers;"
