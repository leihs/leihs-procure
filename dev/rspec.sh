#!/bin/sh -eu
source ./dev/.helpers.sh

# locally run 1 or more specs with rspec, wrapped to work like in rails

SPEC_FILE="$1"

# run server
export LEIHS_HTTP_BASE_URL="http://localhost:$(get_open_port)"

# run server & wait
lein with-profile specs run "run" &
until curl -s "${LEIHS_HTTP_BASE_URL}/procure/status"; do sleep 1 ;done

# run test
bundle exec rspec "$SPEC_FILE"

# shutdown
echo 'STOP SERVER, Errors below can be ignored'
./dev/kill-server.sh
