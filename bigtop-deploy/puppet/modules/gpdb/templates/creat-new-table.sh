#!/bin/sh
export PGOPTIONS="-c gp_session_role=utility";<%= @gp_home %>/bin/psql -p <%= @master_port %> -d "template1" -c "CREATE TABLE bigtopusers (id integer, name text, age numeric);" || true
