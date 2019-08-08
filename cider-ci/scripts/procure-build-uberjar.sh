export PROJECT_DIR="$LEIHS_PROCURE_DIR"
export UBERJAR_NAME="leihs-procure"
LEIN_UBERJAR_PATH="${PROJECT_DIR}/server/target/${UBERJAR_NAME}.jar" # path of the expeced repository uberjar path

function build_uberjar() {
  echo "INFO: building the ${UBERJAR_NAME} uberjar now"
  cd $LEIHS_PROCURE_DIR/client
  export PUBLIC_URL="/procure"
  npm ci || npm install # for building
  npm run build
  cd $LEIHS_PROCURE_DIR/server
  export LEIN_SNAPSHOTS_IN_RELEASE=yes
  lein uberjar
}
