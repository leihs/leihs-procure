(ns leihs.admin.resources.statistics.models
  (:refer-clojure :exclude [str keyword])
  (:require
   [clojure.java.jdbc :as jdbc]
   [clojure.set]
   [leihs.admin.paths :refer [path]]
   [leihs.admin.resources.statistics.shared :as shared]
   [leihs.core.core :refer [keyword str presence]]
   [leihs.core.sql :as sql]
   [logbug.debug :as debug]))

;;; models ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn active_model_cond [active_reservations_cond]
  [:exists
   (-> (sql/select :1)
       (sql/from :reservations)
       (sql/merge-join :items [:= :items.id :reservations.item_id])
       (sql/merge-where [:= :models.id :items.model_id])
       (sql/merge-where active_reservations_cond))])

(defn merge-select-models [query]
  (-> query
      (sql/merge-select [(-> (sql/select :%count.*)
                             (sql/from :models))
                         :models_count])
      (sql/merge-select [(-> (sql/select :%count.*)
                             (sql/from :models)
                             (sql/merge-where (active_model_cond
                                               shared/active_reservations_0m_12m_cond)))
                         :active_models_0m_12m_count])
      (sql/merge-select [(-> (sql/select :%count.*)
                             (sql/from :models)
                             (sql/merge-where (active_model_cond
                                               shared/active_reservations_12m_24m_cond)))
                         :active_models_12m_24m_count])))

(defn get-models [tx]
  {:body (-> {} merge-select-models sql/format
             (->> (jdbc/query tx) first))})

(defn routes [{tx :tx :as request}]
  (get-models tx))

;#### debug ###################################################################

;(debug/debug-ns *ns*)
