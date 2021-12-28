(ns leihs.admin.resources.inventory-pools.inventory-pool.groups.shared
  (:refer-clojure :exclude [str keyword])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.admin.resources.groups.shared :as groups-shared]
    [leihs.admin.paths :refer [path]]
    ))


(def default-query-params
  (merge groups-shared/default-query-params
         {:role "customer"}))

