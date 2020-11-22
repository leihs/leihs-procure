(ns leihs.admin.resources.system.system-admins.paths
  (:refer-clojure :exclude [str keyword])
  (:require
    [leihs.core.core :refer [keyword str presence]]

    [bidi.verbose :refer [branch param leaf]]
    ))

(def paths
  (branch "/system-admins"
          (leaf "/" :system-admins)
          (branch "/"
                  (param :user-id)
                  (leaf "" :system-admin))))

