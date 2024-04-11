(ns leihs.admin.resources.inventory-pools.inventory-pool.groups.shared
  (:require [leihs.admin.resources.groups.shared :as groups-shared]))

(def default-query-params
  (merge groups-shared/default-query-params
         {:role "customer"}))

