(ns leihs.admin.resources.statistics.models
  (:refer-clojure :exclude [str keyword])
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.admin.resources.statistics.shared :as shared]
   [next.jdbc.sql :refer [query] :rename {query jdbc-query}]))

;;; models ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn active_model_cond [active_reservations_cond]
  [:exists
   (-> (sql/select :1)
       (sql/from :reservations)
       (sql/join :items [:= :items.id :reservations.item_id])
       (sql/where [:= :models.id :items.model_id])
       (sql/where active_reservations_cond))])

(defn merge-select-models [query]
  (-> query
      (sql/select [(-> (sql/select :%count.*)
                       (sql/from :models))
                   :models_count])
      (sql/select [(-> (sql/select :%count.*)
                       (sql/from :models)
                       (sql/where (active_model_cond
                                   shared/active_reservations_0m_12m_cond)))
                   :active_models_0m_12m_count])
      (sql/select [(-> (sql/select :%count.*)
                       (sql/from :models)
                       (sql/where (active_model_cond
                                   shared/active_reservations_12m_24m_cond)))
                   :active_models_12m_24m_count])))

(defn get-models [tx]
  {:body (-> {} merge-select-models sql-format
             (->> (jdbc-query tx) first))})

(defn routes [{tx :tx-next :as request}]
  (get-models tx))

;#### debug ###################################################################

;(debug/debug-ns *ns*)
