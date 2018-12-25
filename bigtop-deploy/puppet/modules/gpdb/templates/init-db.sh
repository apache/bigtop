#!/bin/sh
if [ ! -d $1 ]; then
  export LD_LIBRARY_PATH=<%= @gp_home %>/lib:/lib;<%= @gp_home %>/bin/initdb  -E UNICODE -D $1 --locale=en_US.utf8 --max_connections=750 --shared_buffers=128000kB --is_filerep_mirrored=no --backend_output=$1.initdb
fi
