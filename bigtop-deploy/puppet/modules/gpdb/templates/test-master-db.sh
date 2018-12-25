#!/bin/sh
<%= @gp_home %>/bin/psql -p <%= @master_port %> -d "template1" -c "select count(*) from gp_segment_configuration;"
