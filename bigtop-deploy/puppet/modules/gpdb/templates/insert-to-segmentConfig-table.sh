#!/bin/sh
<%= @gp_home %>/bin/psql -p <%= @master_port %> -d "template1" -c "INSERT INTO gp_segment_configuration (dbid, content, role, preferred_role, mode, status, hostname, address, port, replication_port) VALUES ($1, $2, 'p', 'p', 's', 'u', '$4', '$4', $3, null);insert into pg_filespace_entry (fsefsoid, fsedbid, fselocation) values (3052, $1, '$5');" || true
