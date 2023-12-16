(ns leihs.procurement.resources.viewers
  (:require
    ;[clojure.java.jdbc :as jdbc]
    ;        [leihs.procurement.utils.sql :as sql]

    [taoensso.timbre :refer [debug info warn error spy]]

    [leihs.procurement.utils.helpers :refer [my-cast]]

    [honey.sql :refer [format] :rename {format sql-format}]
    [leihs.core.db :as db]
    [next.jdbc :as jdbc]
    [honey.sql.helpers :as sql]

    [clojure.tools.logging :as log]
    [leihs.procurement.resources.users :refer [users-base-query]]
    ))




;(defn my-cast [data]
;  (println ">o> no / 22 / my-cast /debug " data)
;
;
;  (let [
;        data (if (contains? data :id)
;               (assoc data :id [[:cast (:id data) :uuid]])
;               data
;               )
;
;        data (if (contains? data :category_id)
;               (assoc data :category_id [[:cast (:category_id data) :uuid]])
;               data
;               )
;        data (if (contains? data :template_id)
;               (assoc data :template_id [[:cast (:template_id data) :uuid]])
;               data
;               )
;
;        data (if (contains? data :room_id)
;               (assoc data :room_id [[:cast (:room_id data) :uuid]])
;               data
;               )
;
;        data (if (contains? data :order_status)
;               (assoc data :order_status [[:cast (:order_status data) :order_status_enum]])
;               data
;               )
;
;        data (if (contains? data :budget_period_id)
;               (assoc data :budget_period_id [[:cast (:budget_period_id data) :uuid]])
;               data
;               )
;
;        data (if (contains? data :user_id)
;               (assoc data :user_id [[:cast (:user_id data) :uuid]])
;               data
;               )
;
;        ;[[:cast (to-name-and-lower-case a) :order_status_enum]]
;
;        ]
;    (spy data)
;    )
;
;  )

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
