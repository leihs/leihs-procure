(ns leihs.admin.resources.inventory-pools.inventory-pool.delegations.shared
  (:refer-clojure :exclude [str keyword])
  (:require
   [leihs.admin.constants :as defaults]
   [leihs.admin.paths :refer [path]]
   [leihs.core.core :refer [keyword str presence]]))

(def default-query-params
  {:page 1
   :per-page defaults/PER-PAGE
   :term ""
   :membership "member"})

