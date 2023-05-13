#!/bin/sh
export PGOPTIONS="-c gp_session_role=utility";<%= @gp_home %>/bin/psql -p <%= @master_port %> -d "template1" -c "INSERT INTO bigtopusers (name, age) VALUES ('Jack', 28);" || true
