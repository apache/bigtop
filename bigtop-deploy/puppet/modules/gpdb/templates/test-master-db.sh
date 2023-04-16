#!/bin/sh
if [ -f /etc/os-release ]; then
    . /etc/os-release
fi
OS="$ID"

if [ "${OS}" = "fedora" ]; then
    export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/usr/lib/gpdb/lib;
fi

export PGOPTIONS="-c gp_session_role=utility";<%= @gp_home %>/bin/psql -p <%= @master_port %> -d "template1" -c "select age from bigtopusers;"
