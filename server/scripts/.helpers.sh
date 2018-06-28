function get_open_port {
  local SERVER_PORT=10000
  until test -z "$(lsof -i :$SERVER_PORT)" ; do
    SERVER_PORT=$(expr $SERVER_PORT + 1)
  done
  echo $SERVER_PORT
}
