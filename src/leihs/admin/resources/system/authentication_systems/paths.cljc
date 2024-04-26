(ns leihs.admin.resources.system.authentication-systems.paths
  (:require
   [bidi.verbose :refer [branch leaf param]]))

(def paths
  (branch "/authentication-systems"
          (branch "/"
                  (leaf "" :authentication-systems)
                  (leaf "create" :authentication-system-create))
          (branch "/"
                  (param :authentication-system-id)
                  (leaf "" :authentication-system)
                  (leaf "/delete" :authentication-system-delete)
                  (leaf "/edit" :authentication-system-edit)
                  (branch "/groups"
                          (leaf "/" :authentication-system-groups)
                          (branch "/"
                                  (param :group-id)
                                  (leaf "" :authentication-system-group)))
                  (branch "/users"
                          (leaf "/" :authentication-system-users)
                          (branch "/"
                                  (param :user-id)
                                  (leaf "" :authentication-system-user)
                                  (leaf "/edit" :authentication-system-user-edit))))))
