(ns leihs.procurement.resources.inspectors
  (:require [clojure.java.jdbc :as jdbc]
            [leihs.procurement.resources.users :refer [users-base-query]]
            [leihs.procurement.utils.sql :as sql]))

(defn get-inspectors
  [context _ value]
  (jdbc/query (-> context
                  :request
                  :tx)
              (-> users-base-query
                  (sql/merge-where
                    [:in :users.id
                     (-> (sql/select :pci.user_id)
                         (sql/from [:procurement_category_inspectors :pci])
                         (sql/merge-where [:= :pci.category_id (:id value)]))])
                  sql/format)))

(defn delete-inspectors-for-category-id!
  [tx c-id]
  (jdbc/execute! tx
                 (-> (sql/delete-from [:procurement_category_inspectors :pci])
                     (sql/merge-where [:= :pci.category_id c-id])
                     sql/format)))

(defn insert-inspectors!
  [tx row-maps]
  (jdbc/execute! tx
                 (-> (sql/insert-into :procurement_category_inspectors)
                     (sql/values row-maps)
                     sql/format)))

(defn update-inspectors!
  [tx c-id u-ids]
  (delete-inspectors-for-category-id! tx c-id)
  (if (not (empty? u-ids))
    (insert-inspectors! tx
                        (map #(hash-map :user_id % :category_id c-id) u-ids))))

;#### debug ###################################################################
; (logging-config/set-logger! :level :debug)
; (logging-config/set-logger! :level :info)
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
; (debug/undebug-ns *ns*)
