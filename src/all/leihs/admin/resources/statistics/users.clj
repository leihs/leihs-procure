(ns leihs.admin.resources.statistics.users
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.core.core :refer [keyword str presence]])
  (:require
    [clj-logging-config.log4j :as logging-config]
    [clojure.java.jdbc :as jdbc]
    [clojure.set]
    [clojure.tools.logging :as logging]
    [leihs.admin.paths :refer [path]]
    [leihs.admin.resources.statistics.shared :as shared :refer [now]]
    [leihs.core.sql :as sql]
    [logbug.debug :as debug]
    ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn active_users_sub [where-cond]
  (-> (sql/select :%count.*)
      (sql/from :users)
      (sql/merge-where
        [:or
         ; sessions can be destroyed via explicit sign-out; so this is not "quite" correct
         [:exists (-> (sql/select (sql/raw "true"))
                      (sql/from :user_sessions)
                      (sql/merge-where [:= :user_sessions.user_id :users.id])
                      (sql/merge-where where-cond))]
         ; this is reliable but exists only since version 6
         [:exists (-> (sql/select (sql/raw "true"))
                      (sql/from :audited_requests)
                      (sql/merge-where [:= :audited_requests.user_id :users.id])
                      (sql/merge-join :audited_changes [:= :audited_requests.txid :audited_changes.txid])
                      (sql/merge-where [:= :audited_changes.table_name "user_sessions"])
                      (sql/merge-where [:= :audited_changes.tg_op "INSERT"]))]])))

(def sessions_0m_12m_where_cond
  [:and
   [:<= :user_sessions.created_at shared/now]
   [:> :user_sessions.created_at shared/one-year-ago]])

(def sessions_12m_24m_where_cond
  [:and
   [:<= :user_sessions.created_at shared/one-year-ago]
   [:> :user_sessions.created_at shared/two-years-ago]])


(def users-query
  (-> (sql/select [(-> (sql/select :%count.*)
                       (sql/from :users)) :users_count])
      (sql/merge-select [(active_users_sub sessions_0m_12m_where_cond)
                         :active_users_0m_12m_count])
      (sql/merge-select [(active_users_sub sessions_12m_24m_where_cond)
                         :active_users_12m_24m_count])))

(defn get-users [tx]
  {:body (-> users-query sql/format
             (->> (jdbc/query tx) first))})


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn routes [{tx :tx :as request}]
  (get-users tx))

;#### debug ###################################################################
;(logging-config/set-logger! :level :debug)
;(logging-config/set-logger! :level :info)
;(debug/debug-ns *ns*)
