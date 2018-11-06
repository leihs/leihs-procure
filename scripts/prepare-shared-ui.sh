#git submodule update --recursive --init --force leihs-ui
cd leihs-ui
npm ci || npm i
npm run build
npm run build-lib
cd -
