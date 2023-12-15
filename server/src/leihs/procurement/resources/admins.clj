(ns leihs.procurement.resources.admins
  (:require

    [honey.sql :refer [format] :rename {format sql-format}]
    [leihs.core.db :as db]
    [next.jdbc :as jdbc]
    [honey.sql.helpers :as sql]

    [leihs.procurement.utils.helpers :refer [my-cast]]

    [taoensso.timbre :refer [debug info warn error spy]]


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
  (spy (jdbc/execute! (-> context
                          :request
                          :tx-next)
                      (spy (sql-format admins-base-query)))))

(defn delete-all [tx]                                       ;; TODO
  ;(jdbc/delete! tx :procurement_admins [])

  (spy (->>
         (sql/delete-from :procurement_admins)
         sql-format
         spy
         (jdbc/execute! tx)))
  )

(defn update-admins!
  [context args value]
  (let [tx (-> context
               :request
               :tx-next)

        users (:input_data args)

        ]
    (delete-all tx)
    (doseq [d users]
      (spy (jdbc/execute! tx (-> (sql/insert-into :procurement_admins)
                                 (sql/values [(spy (my-cast d))])
                                 sql-format
                                 spy
                                 ))))
    (let [admins (get-admins context args value)
          p (println ">o> update-admins! ???? list<users> ??? admins=" admins)
          ]
      (map #(conj % {:user (->> % :user_id (user/get-user-by-id tx))}) admins))))

;#### debug ###################################################################


; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
; (debug/undebug-ns *ns*)
