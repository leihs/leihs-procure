(ns leihs.admin.resources.statistics.users
  (:refer-clojure :exclude [str keyword])
  (:require
    [clojure.java.jdbc :as jdbc]
    [clojure.set]
    [leihs.admin.paths :refer [path]]
    [leihs.admin.resources.statistics.shared :as shared :refer [now]]
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.sql :as sql]
    [logbug.debug :as debug]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn active_users_sub [before after]
  (-> (sql/select :%count.*)
      (sql/from :users)
      (sql/merge-where
        [:or
         ; sessions can be destroyed via explicit sign-out; so this is not "quite" correct
         [:exists (-> (sql/select (sql/raw "true"))
                      (sql/from :user_sessions)
                      (sql/merge-where [:= :user_sessions.user_id :users.id])
                      (sql/merge-where [:and [:<= :user_sessions.created_at before]
                                        [:> :user_sessions.created_at after]]))]
         ; this is reliable but exists only since version 6
         [:exists (-> (sql/select (sql/raw "true"))
                      (sql/from :audited_requests)
                      (sql/merge-where [:= :audited_requests.user_id :users.id])
                      (sql/merge-join :audited_changes [:= :audited_requests.txid :audited_changes.txid])
                      (sql/merge-where [:= :audited_changes.table_name "user_sessions"])
                      (sql/merge-where [:= :audited_changes.tg_op "INSERT"])
                      (sql/merge-where [:and [:<= :audited_requests.created_at before]
                                        [:> :audited_requests.created_at after]]))]])))

(def users-query
  (-> (sql/select [(-> (sql/select :%count.*)
                       (sql/from :users)) :users_count])
      (sql/merge-select [(active_users_sub shared/now shared/one-year-ago)
                         :active_users_0m_12m_count])
      (sql/merge-select [(active_users_sub shared/one-year-ago shared/two-years-ago)
                         :active_users_12m_24m_count])))

(defn get-users [tx]
  {:body (-> users-query sql/format
             (->> (jdbc/query tx) first))})


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn routes [{tx :tx :as request}]
  (get-users tx))

;#### debug ###################################################################


;(debug/debug-ns *ns*)
