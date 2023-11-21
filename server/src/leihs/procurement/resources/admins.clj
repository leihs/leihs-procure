(ns leihs.procurement.resources.admins
    (:require

      [honey.sql :refer [format] :rename {format sql-format}]
      [leihs.core.db :as db]
      [next.jdbc :as jdbc]
      [honey.sql.helpers :as sql]

      ;        [leihs.procurement.utils.sql :as sql]
      ;[clojure.java.jdbc :as jdbc]

      [leihs.procurement.resources.user :as user]
      [leihs.procurement.resources.users :refer [sql-order-users]]
      ))

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
                         :tx)
                     (sql-format admins-base-query)))

(defn delete-all [tx]                                       ;; TODO
      ;(jdbc/delete! tx :procurement_admins [])

      (->>
        (sql/delete :procurement_admins)
        sql-format
        (jdbc/execute! tx))
      )

(defn update-admins!
      [context args value]
      (let [tx (-> context
                   :request
                   :tx)]
           (delete-all tx)
           (doseq [d (:input_data args)]
                  ;(jdbc/insert! tx :procurement_admins d)         ;; TODO
                  (->> (sql/insert-into :procurement_admins)
                      (sql/set d)
                       sql-format
                      (jdbc/execute! tx)
                      )


                  )
           (let [admins (get-admins context args value)]
                (map #(conj %
                            {:user (->> %
                                        :user_id
                                        (user/get-user-by-id tx))})
                     admins))))

;#### debug ###################################################################


; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
; (debug/undebug-ns *ns*)
