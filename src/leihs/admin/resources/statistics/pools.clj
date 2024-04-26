(ns leihs.admin.resources.statistics.pools
  (:refer-clojure :exclude [str keyword])
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.admin.resources.statistics.shared :as shared]
   [next.jdbc.sql :refer [query] :rename {query jdbc-query}]))

;;; pools ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn active_pool_cond [active_reservations_cond]
  [:exists
   (-> (sql/select :1)
       (sql/from :contracts)
       (sql/where [:= :contracts.inventory_pool_id :inventory_pools.id])
       (sql/join :reservations [:= :contracts.id :reservations.contract_id])
       (sql/where active_reservations_cond))])

(def pools-query
  (-> (sql/select [(-> (sql/select :%count.*)
                       (sql/from :inventory_pools))
                   :pools_count])
      (sql/select [(-> (sql/select :%count.*)
                       (sql/from :inventory_pools)
                       (sql/where (active_pool_cond shared/active_reservations_0m_12m_cond)))
                   :active_pools_0m_12m_count])
      (sql/select [(-> (sql/select :%count.*)
                       (sql/from :inventory_pools)
                       (sql/where (active_pool_cond shared/active_reservations_12m_24m_cond)))
                   :active_pools_12m_24m_count])))

(defn get-pools [tx]
  {:body (-> pools-query sql-format
             (->> (jdbc-query tx) first))})

(defn routes [{tx :tx-next :as request}]
  (get-pools tx))

;#### debug ###################################################################

;(debug/debug-ns *ns*)
