(ns leihs.admin.resources.inventory-pools.inventory-pool.delegations.paths
  (:refer-clojure :exclude [str keyword])
  (:require
   [bidi.verbose :refer [branch param leaf]]
   [leihs.core.core :refer [keyword str presence]]))

(def paths
  (branch "/delegations"
          (leaf "/" :inventory-pool-delegations)
          (branch "/add"
                  (leaf "" :inventory-pool-delegation-create))
          (branch "/"
                  (param :delegation-id)
                  (leaf "" :inventory-pool-delegation)
                  (leaf "/remove" :inventory-pool-delegation-remove)
                  (branch "/edit"
                          (leaf "" :inventory-pool-delegation-edit))
                  (branch "/users"
                          (leaf "/" :inventory-pool-delegation-users)
                          (branch "/"
                                  (param :user-id)
                                  (leaf "" :inventory-pool-delegation-user)))
                  (branch "/groups/"
                          (leaf "" :inventory-pool-delegation-groups)
                          (branch ""
                                  (param :group-id)
                                  (leaf "" :inventory-pool-delegation-group)))
                  (leaf "/suspension" :inventory-pool-delegation-suspension))))
