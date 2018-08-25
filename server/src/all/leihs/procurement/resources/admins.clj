(ns leihs.procurement.resources.admins
  (:require [clojure.java.jdbc :as jdbc]
            [leihs.procurement.resources.user :as user]
            [leihs.procurement.resources.users :refer [sql-order-users]]
            [leihs.procurement.utils.sql :as sql]))

(def admins-base-query
  (-> (sql/select :users.*)
      (sql/from :users)
      (sql/merge-where [:in :users.id
                        (-> (sql/select :procurement_admins.user_id)
                            (sql/from :procurement_admins))])
      sql-order-users))

(defn get-admins
  [context _ _]
  (jdbc/query (-> context
                  :request
                  :tx)
              (sql/format admins-base-query)))

(defn delete-all [tx] (jdbc/delete! tx :procurement_admins []))

(defn update-admins!
  [context args value]
  (let [tx (-> context
               :request
               :tx)]
    (delete-all tx)
    (doseq [d (:input_data args)] (jdbc/insert! tx :procurement_admins d))
    (let [admins (get-admins context args value)]
      (map #(conj %
                  {:user (->> %
                              :user_id
                              (user/get-user-by-id tx))})
        admins))))

;#### debug ###################################################################
; (logging-config/set-logger! :level :debug)
; (logging-config/set-logger! :level :info)
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
; (debug/undebug-ns *ns*)
