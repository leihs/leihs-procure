(ns leihs.admin.main
  (:require
   [leihs.admin.run]
   [leihs.admin.scratch]
   [leihs.core.logging]
   [taoensso.timbre :refer [info]]))

(defn ^:dev/after-load init [& args]
  (leihs.core.logging/init)
  (info  "initializing" 'leihs.admin.main)
  (leihs.admin.run/init!))
