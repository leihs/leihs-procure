(ns leihs.procurement.resources.saved-filters
  (:require [clojure.java.jdbc :as jdbc]
            [leihs.procurement.utils.sql :as sql]))

(defn saved-filters-query
  [user-id]
  (-> (sql/select :procurement_users_filters.*)
      (sql/from :procurement_users_filters)
      (sql/where [:= :procurement_users_filters.user_id user-id])
      sql/format))

(defn get-saved-filters
  [context args value]
  (first (jdbc/query (-> context
                         :request
                         :tx)
                     (saved-filters-query (-> value
                                              :user
                                              :id)))))

(defn get-saved-filters-by-user-id
  [tx user-id]
  (first (jdbc/query tx (saved-filters-query user-id))))

(defn delete-unused
  [tx]
  (jdbc/execute!
    tx
    (-> (sql/delete-from [:procurement_users_filters :puf])
        (sql/merge-where
          [:not
           (sql/call :exists
                     (-> (sql/select true)
                         (sql/from [:procurement_requesters_organizations :pro])
                         (sql/merge-where [:= :pro.user_id :puf.user_id])))])
        sql/format)))

;#### debug ###################################################################
; (logging-config/set-logger! :level :debug)
; (logging-config/set-logger! :level :info)
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
; (debug/undebug-ns *ns*)
