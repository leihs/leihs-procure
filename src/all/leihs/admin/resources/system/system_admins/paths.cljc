(ns leihs.admin.resources.system.system-admins.paths
  (:refer-clojure :exclude [str keyword])
  (:require
    [leihs.core.core :refer [keyword str presence]]

    [bidi.verbose :refer [branch param leaf]]
    ))

(def paths
  (branch "/system-admins"
          (leaf "/" :system-admins)
          (branch "/direct-users"
                  (leaf "/" :system-admin-direct-users)
                  (branch "/"
                          (param :user-id)
                          (leaf "" :system-admins-direct-user)))
          (branch "/groups"
                  (leaf "/" :system-admin-groups)
                  (branch "/"
                          (param :group-id)
                          (leaf "" :system-admins-group)))))

