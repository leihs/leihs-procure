(ns leihs.procurement.resources.admins
  (:require
    [honey.sql :refer [format] :rename {format sql-format}]
    [honey.sql.helpers :as sql]
    [leihs.core.utils :refer [my-cast]]
    [leihs.procurement.resources.user :as user]
    [leihs.procurement.resources.users :refer [sql-order-users]]
    [next.jdbc :as jdbc]
    [taoensso.timbre :refer [debug error info spy warn]]))

(def admins-base-query
  (-> (sql/select :users.*)
      (sql/from :users)
      (sql/where [:in :users.id
                  (-> (sql/select :procurement_admins.user_id)
                      (sql/from :procurement_admins))])
      sql-order-users))

(defn get-admins
  [context _ _]
  (jdbc/execute! (-> context
                     :request
                     :tx-next)
                 (sql-format admins-base-query)))

(defn delete-all [tx]
  (->> (sql/delete-from :procurement_admins)
       sql-format
       (jdbc/execute! tx)))

(defn update-admins!
  [context args value]
  (let [tx (-> context
               :request
               :tx-next)
        users (:input_data args)]
    (delete-all tx)
    (doseq [d users]
      (jdbc/execute! tx (-> (sql/insert-into :procurement_admins)
                            (sql/values [(my-cast d)])
                            sql-format)))
    (let [admins (get-admins context args value)]
      (map #(conj % {:user (->> % :user_id (user/get-user-by-id tx))}) admins))))

;#### debug ###################################################################


; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
; (debug/undebug-ns *ns*)
