#!/bin/sh

export LEIHS_DATABASE_URL=jdbc:postgresql://localhost:5432/leihs_test?max-pool-size=5
export LEIHS_HTTP_BASE_URL=http://localhost:3333
lein run "run"
