(ns leihs.admin.resources.statistics.users
  (:require
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.admin.resources.statistics.shared :as shared :refer [now]]
   [next.jdbc.sql :refer [query] :rename {query jdbc-query}]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn active_users_sub [before after]
  (-> (sql/select :%count.*)
      (sql/from :users)
      (sql/where
       [:or
         ; sessions can be destroyed via explicit sign-out; so this is not "quite" correct
        [:exists (-> (sql/select [[:raw "true"]])
                     (sql/from :user_sessions)
                     (sql/where [:= :user_sessions.user_id :users.id])
                     (sql/where [:and [:<= :user_sessions.created_at before]
                                 [:> :user_sessions.created_at after]]))]
         ; this is reliable but exists only since version 6
        [:exists (-> (sql/select [[:raw "true"]])
                     (sql/from :audited_requests)
                     (sql/where [:= :audited_requests.user_id :users.id])
                     (sql/join :audited_changes [:= :audited_requests.txid :audited_changes.txid])
                     (sql/where [:= :audited_changes.table_name "user_sessions"])
                     (sql/where [:= :audited_changes.tg_op "INSERT"])
                     (sql/where [:and [:<= :audited_requests.created_at before]
                                 [:> :audited_requests.created_at after]]))]])))

(def users-query
  (-> (sql/select [(-> (sql/select :%count.*)
                       (sql/from :users)) :users_count])
      (sql/select [(active_users_sub shared/now shared/one-year-ago)
                   :active_users_0m_12m_count])
      (sql/select [(active_users_sub shared/one-year-ago shared/two-years-ago)
                   :active_users_12m_24m_count])))

(defn get-users [tx]
  {:body (-> users-query sql-format
             (->> (jdbc-query tx) first))})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn routes [{tx :tx :as request}]
  (get-users tx))

;#### debug ###################################################################

;(debug/debug-ns *ns*)
