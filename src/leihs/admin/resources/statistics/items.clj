(ns leihs.admin.resources.statistics.items
  (:refer-clojure :exclude [str keyword])
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.admin.resources.statistics.shared :as shared]
   [next.jdbc.sql :refer [query] :rename {query jdbc-query}]))

;;; items ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn active_item_cond [active_reservations_cond]
  [:exists
   (-> (sql/select :1)
       (sql/from :reservations)
       (sql/where [:= :items.id :reservations.item_id])
       (sql/where active_reservations_cond))])

(defn merge-select-items [query]
  (-> query
      (sql/select [(-> (sql/select :%count.*)
                       (sql/from :items))
                   :items_count])
      (sql/select [(-> (sql/select :%count.*)
                       (sql/from :items)
                       (sql/where (active_item_cond
                                   shared/active_reservations_0m_12m_cond)))
                   :active_items_0m_12m_count])
      (sql/select [(-> (sql/select :%count.*)
                       (sql/from :items)
                       (sql/where (active_item_cond
                                   shared/active_reservations_12m_24m_cond)))
                   :active_items_12m_24m_count])))

(defn routes [{tx :tx-next :as request}]
  {:body (-> {} merge-select-items sql-format
             (->> (jdbc-query tx) first))})

;#### debug ###################################################################

;(debug/debug-ns *ns*)
