(ns leihs.admin.main
  (:require [leihs.admin.run]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(leihs.admin.run/init!)
