(ns leihs.admin.main
  (:require
    [leihs.admin.run]
    [taoensso.timbre :as logging]
    ))

(defn ^:dev/after-load init [& args]
  (logging/info  "initializing" 'leihs.admin.main)
  (leihs.admin.run/init!)
  )
