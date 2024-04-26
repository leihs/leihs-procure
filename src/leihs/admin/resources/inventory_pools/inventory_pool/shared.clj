(ns leihs.admin.resources.inventory-pools.inventory-pool.shared
  (:refer-clojure :exclude [str keyword])
  (:require
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.core.core :refer [presence]]
   [next.jdbc.sql :refer [query] :rename {query jdbc-query}]))

(defn normalized-inventory-pool-id! [inventory-pool-id tx]
  "Get the id, i.e. the pkey, given either the id or the org_id and
  enforce some sanity checks like uniqueness and presence"
  (assert (presence inventory-pool-id) "inventory-pool-id must not be empty!")
  (let [where-clause (when (instance? java.util.UUID inventory-pool-id)
                       [:or
                        [:= :inventory-pools.id inventory-pool-id]
                        [:= :inventory-pools.name [:cast inventory-pool-id :text]]])
        ids (-> (sql/select :id)
                (sql/from :inventory-pools)
                (sql/where where-clause)
                sql-format
                (->> (jdbc-query tx)
                     (map :id)))]
    (assert (= 1 (count ids))
            "exactly one inventory-pool must match the given inventory-pool-id either name, or id")
    (first ids)))
