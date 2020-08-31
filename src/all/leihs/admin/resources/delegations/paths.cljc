(ns leihs.admin.resources.delegations.paths
  (:refer-clojure :exclude [str keyword])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [bidi.verbose :refer [branch param leaf]]))


(def paths
  (branch "/delegations"
          (leaf "/" :delegations)
          (branch "/add"
                  (leaf "" :delegation-add)
                  (leaf "/choose-responsible-user"
                        :delegation-add-choose-responsible-user))
          (branch "/"
                  (param :delegation-id)
                  (leaf "" :delegation)
                  (leaf "/delete" :delegation-delete)
                  (branch "/edit"
                          (leaf "" :delegation-edit)
                          (leaf "/choose-responsible-user"
                                :delegation-edit-choose-responsible-user))
                  (branch "/users"
                          (leaf "/" :delegation-users)
                          (branch "/"
                                  (param :user-id)
                                  (leaf "" :delegation-user)))
                  (branch "/groups/"
                          (leaf "" :delegation-groups)
                          (branch ""
                                  (param :group-id)
                                  (leaf "" :delegation-group))))))

