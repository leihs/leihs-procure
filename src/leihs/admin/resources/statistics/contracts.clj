(ns leihs.admin.resources.statistics.contracts
  (:require
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.admin.resources.statistics.shared :as shared]
   [next.jdbc.sql :refer [query] :rename {query jdbc-query}]))

;;; contracts ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn active_contract_cond [active_reservations_cond]
  [:exists
   (-> (sql/select :1)
       (sql/from :reservations)
       (sql/where [:= :contracts.id :reservations.contract_id])
       (sql/where active_reservations_cond))])

(defn merge-select-contracts [query]
  (-> query
      (sql/select [(-> (sql/select :%count.*)
                       (sql/from :contracts))
                   :contracts_count])
      (sql/select [(-> (sql/select :%count.*)
                       (sql/from :contracts)
                       (sql/where (active_contract_cond
                                   shared/active_reservations_0m_12m_cond)))
                   :active_contracts_0m_12m_count])
      (sql/select [(-> (sql/select :%count.*)
                       (sql/from :contracts)
                       (sql/where (active_contract_cond
                                   shared/active_reservations_12m_24m_cond)))
                   :active_contracts_12m_24m_count])))

(defn routes [{handler-key :handler-key tx :tx :as request}]
  {:body (-> {} merge-select-contracts sql-format
             (->> (jdbc-query tx) first))})

;#### debug ###################################################################

;(debug/debug-ns *ns*)
