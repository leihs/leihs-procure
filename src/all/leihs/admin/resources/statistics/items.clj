(ns leihs.admin.resources.statistics.items
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.core.core :refer [keyword str presence]])
  (:require
    [clj-logging-config.log4j :as logging-config]
    [clojure.java.jdbc :as jdbc]
    [clojure.set]
    [clojure.tools.logging :as logging]
    [compojure.core :as cpj]
    [leihs.admin.paths :refer [path]]
    [leihs.admin.resources.statistics.shared :as shared]
    [leihs.core.sql :as sql]
    [logbug.debug :as debug]))



;;; items ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn active_item_cond [active_reservations_cond]
  [:exists
   (-> (sql/select :1)
       (sql/from :reservations)
       (sql/merge-where [:= :items.id :reservations.item_id])
       (sql/merge-where active_reservations_cond))])

(defn merge-select-items [query]
  (-> query
      (sql/merge-select [(-> (sql/select :%count.*)
                             (sql/from :items))
                         :items_count])
      (sql/merge-select [(-> (sql/select :%count.*)
                             (sql/from :items)
                             (sql/merge-where (active_item_cond
                                                shared/active_reservations_0m_12m_cond)))
                         :active_items_0m_12m_count])
      (sql/merge-select [(-> (sql/select :%count.*)
                             (sql/from :items)
                             (sql/merge-where (active_item_cond
                                                shared/active_reservations_12m_24m_cond)))
                         :active_items_12m_24m_count])))


(defn routes [{tx :tx :as request}]
  {:body (-> {} merge-select-items sql/format
             (->> (jdbc/query tx) first))})

;#### debug ###################################################################
;(logging-config/set-logger! :level :debug)
;(logging-config/set-logger! :level :info)
;(debug/debug-ns *ns*)
