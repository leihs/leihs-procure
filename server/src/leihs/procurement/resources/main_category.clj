(ns leihs.procurement.resources.main-category
  (:require
    ;[clojure.java.jdbc :as jdbc]

    [honey.sql :refer [format] :rename {format sql-format}]
    [leihs.core.db :as db]
    [next.jdbc :as jdbc]

    [leihs.procurement.utils.helpers :refer [my-cast]]

    [taoensso.timbre :refer [debug info warn error spy]]


    [honey.sql.helpers :as sql]

    [clojure.tools.logging :as log]
    [leihs.procurement.resources [budget-limits :as budget-limits]
     [categories :as categories] [image :as image] [images :as images]
     [uploads :as uploads]]
    [leihs.procurement.utils [helpers :refer [submap?]]
     [sql :as sqlo]
     ]
    ))

(def main-category-base-query
  (-> (sql/select :procurement_main_categories.*)
      (sql/from :procurement_main_categories)))

(defn main-category-query-by-id
  [id]
  (println ">debug 24")

  (-> main-category-base-query
      (sql/where [:= :procurement_main_categories.id [:cast id :uuid]])
      sql-format))

(defn main-category-query-by-name
  [mc-name]
  (println ">debug 23")

  (-> main-category-base-query
      (sql/where [:= :procurement_main_categories.name mc-name])
      sql-format))

(defn get-main-category
  [context _ value]
  (println ">debug 22")

  (-> value
      :main_category_id
      main-category-query-by-id
      (->> (jdbc/execute! (-> context
                              :request
                              :tx-next)))
      first))

(defn get-main-category-by-name
  [tx mc-name]

  (println ">debug 21")

  (first (jdbc/execute! tx (main-category-query-by-name mc-name))))

(defn insert!
  [tx mc]
  (println ">debug 25")
  (jdbc/execute! tx
                 (-> (sql/insert-into :procurement_main_categories)
                     (sql/values [mc])
                     sql-format
                     log/spy)))

(defn- filter-images [m is] (filter #(submap? m %) is))

(defn deal-with-image!
  [tx mc-id images]

  (println ">debug 26")

  (log/info images)
  (when-let [new-image-upload (-> {:to_delete false, :typename "Upload"}
                                  (filter-images images)
                                  first)]
    (jdbc/execute! tx
                   (-> (sql/delete-from :procurement_images)
                       (sql/where [:= :procurement_images.main_category_id [:cast mc-id :uuid]])
                       sql-format))
    (image/create-for-main-category-id-and-upload! tx mc-id new-image-upload))
  (when-let [uploads-to-delete (-> {:to_delete true, :typename "Upload"}
                                   (filter-images images)
                                   not-empty)]
    (uploads/delete! tx (map :id uploads-to-delete))))





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

(defn update!
  [tx mc]

  (println ">debug 27")

  (println ">o> abc tocheck >>" mc)
  (spy (jdbc/execute! tx
                      (-> (sql/update :procurement_main_categories)
                          (sqlo/sset (spy (my-cast mc)))
                          (sql/where [:= :procurement_main_categories.id [:cast (spy (:id mc)) :uuid]])
                          sql-format))))



(defn can-delete?
  [context _ value]

  (println ">debug 28")

  (println ">> can-delete3")
  (spy (->
         (jdbc/execute-one!
           (-> context
               :request
               :tx-next) (-> [:and
                              [:not
                               [:exists
                                (-> (sql/select true)
                                    (sql/from [:procurement_requests :pr])
                                    (sql/join [:procurement_categories :pc]
                                              [:= :pc.id :pr.category_id])
                                    (sql/where [:= :pc.main_category_id
                                                [:cast (:id value) :uuid]]))]]
                              [:not
                               [:exists
                                (-> (sql/select true)
                                    (sql/from [:procurement_templates :pt])
                                    (sql/join [:procurement_categories :pc]
                                              [:= :pc.id :pt.category_id])
                                    (sql/where [:= :pc.main_category_id
                                                [:cast (:id value) :uuid]]))]]]
                             (vector :result)
                             sql/select
                             sql-format
                             spy
                             ))
         :result)))

(defn delete!
  [tx id]


  (println ">debug 29")

  (jdbc/execute! tx
                 (-> (sql/delete-from :procurement_main_categories)
                     (sql/where [:= :procurement_main_categories.id [:cast id :uuid]])
                     sql-format
                     spy
                     )))
