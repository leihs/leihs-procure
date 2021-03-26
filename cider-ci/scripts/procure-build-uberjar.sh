export PROJECT_DIR="$LEIHS_PROCURE_DIR"
export UBERJAR_NAME="leihs-procure"
LEIN_UBERJAR_PATH="${PROJECT_DIR}/server/target/${UBERJAR_NAME}.jar" # path of the expeced repository uberjar path

function build_uberjar() {
  echo "INFO: building the ${UBERJAR_NAME} uberjar now"
  cd $LEIHS_PROCURE_DIR/client
  export PUBLIC_URL="/procure"
  # TMP workaround:
  { cd leihs-ui/bootstrap-theme-leihs && npm install && cd - ;} || exit 1
  { cd leihs-ui && npm install && cd - ;} || exit 1
  npm install || exit 1
  npm run ci-build || exit 1
  cd $LEIHS_PROCURE_DIR/server
  export LEIN_SNAPSHOTS_IN_RELEASE=yes
  lein uberjar
}
