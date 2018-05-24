#!/bin/sh -exu

# NOTE: this is for terminating in CI,
# in other situations a less drastic signal than 9 might be more appropiate.

SERVER_PID="./tmp/server_pid"
kill -9 $(cat "$SERVER_PID")
rm -f "$SERVER_PID"
