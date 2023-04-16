#!/bin/sh
if [ -f /etc/os-release ]; then
    . /etc/os-release
fi
OS="$ID"

if [ "${OS}" = "fedora" ]; then
    export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/usr/lib/gpdb/lib;
fi

if [ ! -d $1 ]; then
  export LD_LIBRARY_PATH=<%= @gp_home %>/lib:/lib;<%= @gp_home %>/bin/initdb -E UNICODE -D $1 --max_connections=750 --shared_buffers=128000kB --backend_output=$1.initdb
fi
