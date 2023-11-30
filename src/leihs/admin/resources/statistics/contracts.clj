(ns leihs.admin.resources.statistics.contracts
  (:refer-clojure :exclude [str keyword])
  (:require
   [clojure.java.jdbc :as jdbc]
   [clojure.set]
   [compojure.core :as cpj]
   [leihs.admin.paths :refer [path]]
   [leihs.admin.resources.statistics.shared :as shared]
   [leihs.core.core :refer [keyword str presence]]
   [leihs.core.sql :as sql]
   [logbug.debug :as debug]))

;;; contracts ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn active_contract_cond [active_reservations_cond]
  [:exists
   (-> (sql/select :1)
       (sql/from :reservations)
       (sql/merge-where [:= :contracts.id :reservations.contract_id])
       (sql/merge-where active_reservations_cond))])

(defn merge-select-contracts [query]
  (-> query
      (sql/merge-select [(-> (sql/select :%count.*)
                             (sql/from :contracts))
                         :contracts_count])
      (sql/merge-select [(-> (sql/select :%count.*)
                             (sql/from :contracts)
                             (sql/merge-where (active_contract_cond
                                               shared/active_reservations_0m_12m_cond)))
                         :active_contracts_0m_12m_count])
      (sql/merge-select [(-> (sql/select :%count.*)
                             (sql/from :contracts)
                             (sql/merge-where (active_contract_cond
                                               shared/active_reservations_12m_24m_cond)))
                         :active_contracts_12m_24m_count])))

(defn routes [{handler-key :handler-key tx :tx
               :as request}]
  {:body (-> {} merge-select-contracts sql/format
             (->> (jdbc/query tx) first))})

;#### debug ###################################################################

;(debug/debug-ns *ns*)
