(ns leihs.admin.resources.inventory-pools.inventory-pool.shared
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.core.core :refer [keyword str presence]])
  (:require
    [leihs.admin.paths :refer [path]]
    [leihs.admin.utils.regex :as regex]
    [leihs.core.sql :as sql]
    [leihs.admin.utils.jdbc :as utils.jdbc]

    [clojure.java.jdbc :as jdbc]
    [compojure.core :as cpj]
    [clojure.set :as set]

    [clj-logging-config.log4j :as logging-config]
    [clojure.tools.logging :as logging]
    [logbug.debug :as debug]
    ))

(defn normalized-inventory-pool-id! [inventory-pool-id tx]
  "Get the id, i.e. the pkey, given either the id or the org_id and
  enforce some sanity checks like uniqueness and presence"
  (assert (presence inventory-pool-id) "inventory-pool-id must not be empty!")
  (let [inventory-pool-id (str inventory-pool-id)
        where-clause  (if (re-matches regex/uuid-pattern inventory-pool-id)
                        [:or
                         [:= :inventory-pools.id inventory-pool-id]
                         [:= :inventory-pools.name inventory-pool-id]])
        ids (->> (-> (sql/select :id)
                     (sql/from :inventory-pools)
                     (sql/merge-where where-clause)
                     sql/format)
                 (jdbc/query tx)
                 (map :id))]
    (assert (= 1 (count ids))
            "exactly one inventory-pool must match the given inventory-pool-id either name, or id")
    (first ids)))
