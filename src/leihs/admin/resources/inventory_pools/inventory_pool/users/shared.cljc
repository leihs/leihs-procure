(ns leihs.admin.resources.inventory-pools.inventory-pool.users.shared
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.core.core :refer [keyword str presence]]))

(def default-query-params
  {:role "customer"
   :suspension "any" })
