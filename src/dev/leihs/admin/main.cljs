(ns ^:figwheel-no-load leihs.admin.main
  (:require
    [leihs.admin.run]
    [leihs.admin.html]
    [leihs.core.requests.core :as requests]
    [figwheel.client :as figwheel :include-macros true]))

(enable-console-print!)

(figwheel/watch-and-reload
  :websocket-url "ws://localhost:3222/figwheel-ws"
  :jsload-callback leihs.admin.html/mount)

(leihs.admin.run/init!)

(reset! requests/request-delay* 750)
