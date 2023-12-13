(ns leihs.procurement.resources.inspectors
  (:require
    ;[clojure.java.jdbc :as jdbc]
    ;        [leihs.procurement.utils.sql :as sql]

        [taoensso.timbre :refer [debug info warn error spy]]

        [leihs.procurement.utils.helpers :refer [my-cast]]


    [honey.sql :refer [format] :rename {format sql-format}]
    [leihs.core.db :as db]
    [next.jdbc :as jdbc]
    [honey.sql.helpers :as sql]

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



(defn get-inspectors
  [context _ value]

  (println ">o> tocheck1 true/false")

  (spy (jdbc/execute! (-> context
                          :request
                          :tx-next)
                      (-> users-base-query
                          (sql/where [:in :users.id (-> (sql/select :pci.user_id)
                                                        (sql/from [:procurement_category_inspectors :pci])
                                                        (sql/where [:= :pci.category_id [:cast (:id value) :uuid]]))])
                          sql-format
                          spy)
           )

       ))

(defn delete-inspectors-for-category-id!
  [tx c-id]

  (println ">o> tocheck2 true/false")

  (spy (jdbc/execute! tx (-> (sql/delete-from :procurement_category_inspectors :pci)
                             (sql/where [:= :pci.category_id [:cast (spy c-id) :uuid]])
                             sql-format
                             spy
                             ))))

(defn insert-inspectors!
  [tx row-maps]

  (println ">o> tocheck3 true/false")
  (println ">o> tocheck3 true/false" row-maps)

  (spy (jdbc/execute! tx (-> (sql/insert-into :procurement_category_inspectors)
                             (sql/values (map #(my-cast %) row-maps))
                             sql-format))))

(defn update-inspectors!
  [tx c-id u-ids]
  (delete-inspectors-for-category-id! tx c-id)
  (if (not (empty? (spy u-ids)))
    (insert-inspectors! tx (map #(hash-map :user_id % :category_id c-id) u-ids))))

