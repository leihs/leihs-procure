#!/bin/sh

{ npm i && clear ;}
{ npm run build && clear ;}
echo $(npx serve -p 5000 && clear) &

echo 'starting local proxy'
npx micro-proxy -p 9000 -r proxy-dev.json
