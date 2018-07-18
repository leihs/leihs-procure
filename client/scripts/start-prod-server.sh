#!/bin/sh

{ npm i && clear ;}
{ npm run build && clear ;}
echo $(npx serve -s -p 5000 ./build && clear) &

echo 'starting local proxy'
npx micro-proxy -p 9000 -r proxy-dev.json
