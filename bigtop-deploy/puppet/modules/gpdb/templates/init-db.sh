#!/bin/sh
if [ ! -d $1 ]; then
  export LD_LIBRARY_PATH=<%= @gp_home %>/lib:/lib;<%= @gp_home %>/bin/initdb -E UNICODE -D $1 --max_connections=750 --shared_buffers=128000kB
fi
