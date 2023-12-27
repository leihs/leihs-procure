(ns leihs.procurement.resources.viewers
  (:require
    ;[clojure.java.jdbc :as jdbc]
    ;        [leihs.procurement.utils.sql :as sql]

    [taoensso.timbre :refer [debug info warn error spy]]

    [leihs.core.utils :refer [my-cast]]

    [honey.sql :refer [format] :rename {format sql-format}]
    [leihs.core.db :as db]
    [next.jdbc :as jdbc]
    [honey.sql.helpers :as sql]

    [clojure.tools.logging :as log]
    [leihs.procurement.resources.users :refer [users-base-query]]
    ))

(defn get-viewers
  [context _ value]
  (jdbc/execute! (-> context
                     :request
                     :tx-next)
                 (-> users-base-query (sql/where [:in :users.id
                                                  (-> (sql/select :pcv.user_id)
                                                      (sql/from [:procurement_category_viewers :pcv])
                                                      (sql/where [:= :pcv.category_id [:cast (:id value) :uuid]]))])
                     sql-format
                     spy)))

(defn delete-viewers-for-category-id!
  [tx c-id]

  (let [
        result (spy (jdbc/execute-one! tx (-> (sql/delete-from :procurement_category_viewers :pcv)
                                         (sql/where [:= :pcv.category_id [:cast c-id :uuid]])
                                         sql-format
                                         ;spy
                                         )))

        res (spy (:update-count result ))
        result (spy (:next.jdbc/update-count result ))

        ]
    (spy (list result))
    )

  ;(spy (-> (spy (jdbc/execute-one! tx (-> (sql/delete-from :procurement_category_viewers :pcv)
  ;                               (sql/where [:= :pcv.category_id [:cast c-id :uuid]])
  ;                               sql-format
  ;                               ;spy
  ;                               )))
  ;         ;:next.jdbc/update-count
  ;         :update-count
  ;         list
  ;         ))
  )

(defn insert-viewers!
  [tx row-maps]
  (println ">o> >tocheck>" row-maps)
  (spy (-> (spy (jdbc/execute-one! tx (-> (sql/insert-into :procurement_category_viewers)
                                          (sql/values (map #(my-cast %) row-maps))
                                          sql-format
                                          ;spy
                                          )))
           ;:update-count
           :next.jdbc/update-count
           list
           )))

(defn update-viewers!
  [tx c-id u-ids]

  (println ">o> ??? update-viewers! u-ids=" u-ids)
  (println ">o> ??? update-viewers! c-id=" c-id)

  (spy (-> (delete-viewers-for-category-id! tx c-id)
           ;:update-count
           ;list
           ))
  (if (spy (not (empty? u-ids)))
    (spy (insert-viewers! tx (map #(hash-map :user_id % :category_id c-id) u-ids)))
    ))
