(ns leihs.admin.resources.statistics.pools
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.core.core :refer [keyword str presence]])
  (:require
    [clj-logging-config.log4j :as logging-config]
    [clojure.java.jdbc :as jdbc]
    [clojure.set]
    [clojure.tools.logging :as logging]
    [leihs.admin.paths :refer [path]]
    [leihs.admin.resources.statistics.shared :as shared]
    [leihs.core.sql :as sql]
    [logbug.debug :as debug]
    ))


;;; pools ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn active_pool_cond [active_reservations_cond]
  [:exists
   (-> (sql/select :1)
       (sql/from :contracts)
       (sql/merge-where [:= :contracts.inventory_pool_id :inventory_pools.id])
       (sql/merge-join :reservations [:= :contracts.id :reservations.contract_id])
       (sql/merge-where active_reservations_cond))])

(def pools-query
  (-> (sql/merge-select [(-> (sql/select :%count.*)
                             (sql/from :inventory_pools)) :pools_count])
      (sql/merge-select [(-> (sql/select :%count.*)
                             (sql/from :inventory_pools)
                             (sql/merge-where (active_pool_cond shared/active_reservations_0m_12m_cond))
                             ) :active_pools_0m_12m_count])
      (sql/merge-select [(-> (sql/select :%count.*)
                             (sql/from :inventory_pools)
                             (sql/merge-where (active_pool_cond shared/active_reservations_12m_24m_cond))
                             ) :active_pools_12m_24m_count])))

(defn get-pools [tx]
  {:body (-> pools-query sql/format
             (->> (jdbc/query tx) first))})



(defn routes [{tx :tx :as request}]
  (get-pools tx))

;#### debug ###################################################################
;(logging-config/set-logger! :level :debug)
;(logging-config/set-logger! :level :info)
;(debug/debug-ns *ns*)
