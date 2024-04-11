(ns leihs.admin.run
  (:require [leihs.admin.html :as html]
            [leihs.admin.routes :as routes]
            [leihs.core.user.front :refer [load-user-data-from-dom]]
            [taoensso.timbre :refer [info]]))

(defn init! []
  (info  "initializing")
  (load-user-data-from-dom)
  (routes/init)
  (html/mount))
