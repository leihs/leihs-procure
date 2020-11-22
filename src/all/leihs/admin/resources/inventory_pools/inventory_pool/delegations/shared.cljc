(ns leihs.admin.resources.inventory-pools.inventory-pool.delegations.shared
  (:refer-clojure :exclude [str keyword])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.admin.paths :refer [path]]
    [leihs.admin.defaults :as defaults]))

(def default-query-params
  {:page 1
   :per-page defaults/PER-PAGE
   :term ""
   :membership "member"})

