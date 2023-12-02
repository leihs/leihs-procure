(ns leihs.procurement.resources.user
  (:require
    ;[clojure.java.jdbc :as jdbc]
    ;        [leihs.procurement.utils.sql :as sql]

    [honey.sql :refer [format] :rename {format sql-format}]
    [leihs.core.db :as db]
    [next.jdbc :as jdbc]
    [taoensso.timbre :refer [debug info warn error spy]]

    [honey.sql.helpers :as sql]
    ))

(def user-base-query
  (-> (sql/select :id :firstname :lastname)
      (sql/from :users)))

(defn get-user
  [context _ value]
  ((jdbc/execute-one! (-> context
                          :request
                          :tx-next)
                      (-> user-base-query
                          (sql/where [:= :users.id
                                      [:cast (or (:user_id value)  ; for
                                          ; RequesterOrganization
                                          (:value value)    ; for RequestFieldUser
                                          ) :uuid]])
                          sql-format))))



(comment

  ;[honey.sql :refer [format] :rename {format sql-format}]
  ;[leihs.core.db :as db]
  ;[next.jdbc :as jdbc]
  ;[honey.sql.helpers :as sql]

  (let [
        ;user-id #uuid "3eaba478-f710-4cb8-bc87-54921a27e3bb" ;; >>3 [{:has_entry true}]
        ;user-id #uuid "3eaba478-f710-4cb8-bc87-54921a27e3b2" ;; >>3 []
        user-id #uuid "d61a3ce9-ba03-4ad6-8acf-cba00aee9ed6" ;; >>3 []

        id user-id

        user-id nil                                         ;; >>3 []
        auth-entity {:user_id user-id}

        c-id nil
        c-id #uuid "1efc2279-bc42-490c-b004-dca03813a6ef"

        tx (db/get-ds-next)


        sql (-> user-base-query
                (sql/where [:= :users.id id])
                sql-format)

        p (println sql)

        result (jdbc/execute-one! tx sql)

        p (println result)

        ]
    ;(inspector? tx auth-entity c-id)
    )

  )


(defn get-user-by-id
  [tx id]
  (spy id)
  (jdbc/execute-one! tx
                     (-> user-base-query
                         ;(sql/where [:= :users.id (:cast id :uuid)])
                         (sql/where [:= :id [:cast id :uuid]])
                         sql-format)))
