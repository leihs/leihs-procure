#!/bin/sh -eu
source ./dev/.helpers.sh

export LEIHS_HTTP_BASE_URL="http://localhost:$(get_open_port)"
lein with-profile specs run "run"
