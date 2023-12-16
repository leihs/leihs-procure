(ns leihs.procurement.resources.categories
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log]
            [leihs.procurement.authorization :as authorization]

            [taoensso.timbre :refer [debug info warn error spy]]

            [leihs.procurement.permissions.user :as user-perms]
            [leihs.procurement.resources [category :as category]
             [inspectors :as inspectors] [viewers :as viewers]]
            [leihs.procurement.utils.sql :as sql]))

(def categories-base-query
  (-> (sql/select :procurement_categories.*)
      (sql/from :procurement_categories)
      (sql/order-by [:procurement_categories.name :asc])))

(defn categories-query
  [context arguments value]

  (println ">oo> tocheck value" value)

  (let [id (:id arguments)
        p (println ">o> tocheck (not nil, multiple??)" id)
        inspected-by-auth-user (:inspected_by_auth_user arguments)
        main-category-id (:id value)]
    (sql/format
      (cond-> categories-base-query
              id (sql/merge-where [:in :procurement_categories.id id])
              main-category-id (sql/merge-where
                                 [:= :procurement_categories.main_category_id
                                  main-category-id])
              inspected-by-auth-user
              (-> (sql/merge-join :procurement_category_inspectors
                                  [:= :procurement_category_inspectors.category_id
                                   :procurement_categories.id])
                  (sql/merge-where [:= :procurement_category_inspectors.user_id
                                    (-> context
                                        :request
                                        :authenticated-entity
                                        :user_id)]))))))

(defn get-categories-for-ids
  [tx ids]
  (-> categories-base-query
      (sql/merge-where [:in :procurement_categories.id ids])
      sql/format
      (->> (jdbc/query tx))))

(defn get-for-main-category-id
  [tx main-cat-id]
  (-> categories-base-query
      (sql/merge-where [:= :procurement_categories.main_category_id
                        main-cat-id])
      sql/format
      spy
      (->> (jdbc/query tx))))

(defn get-categories
  [context arguments value]
  (if (= (:id arguments) [])
    []
    (->> (categories-query context arguments value)
         spy
         (jdbc/query (-> context
                         :request
                         :tx)))))

(defn delete-categories-for-main-category-id-and-not-in-ids!
  [tx mc-id ids]
  (jdbc/execute!
    tx
    (-> (sql/delete-from :procurement_categories)
        (sql/merge-where [:= :procurement_categories.main_category_id mc-id])
        (cond-> (not (empty? ids)) (sql/merge-where
                                     [:not-in :procurement_categories.id ids]))
        spy
        sql/format)))

(defn update-categories!
  [tx mc-id cs]
  (loop [[c & rest-cs] cs
         c-ids []]
    (if c
      (do (if (:id c)
            (category/update-category! tx (dissoc c :inspectors :viewers))
            (category/insert-category! tx (dissoc c :id :inspectors :viewers)))
          (let [c-id (or (:id c)
                         (as-> c <>
                           (select-keys <> [:name :main_category_id])
                           (category/get-category tx <>)
                           (:id <>)))]
            (inspectors/update-inspectors! tx c-id (:inspectors c))
            (viewers/update-viewers! tx c-id (:viewers c))
            (recur rest-cs (conj c-ids c-id))))
      (delete-categories-for-main-category-id-and-not-in-ids! tx mc-id c-ids))))

(defn update-categories-viewers!
  [context args value]
  (spy (let [request (:request context)
             tx (:tx request)
             auth-user (:authenticated-entity request)
             categories (:input_data args)]
         (loop [[c & rest-cs] categories]
           (if-let [c-id (:id c)]
             (do (authorization/authorize-and-apply
                   #(spy (viewers/update-viewers! tx c-id (:viewers c)))
                 :if-any
                 [
                  #(spy (user-perms/admin? tx auth-user))
                  #(spy (user-perms/inspector? tx auth-user c-id))
                  ])
             (recur rest-cs))
           (spy (jdbc/query tx
                            (-> categories-base-query
                                (sql/merge-where [:in :procurement_categories.id
                                                  (map :id categories)])
                                sql/format)))

           )))) )

;2023-12-16T16:23:13.056Z NX-41294 DEBUG [leihs.procurement.resources.categories:110] - (user-perms/admin? tx auth-user) => false
;2023-12-16T16:23:13.057Z NX-41294 DEBUG [leihs.procurement.resources.categories:111] - (user-perms/inspector? tx auth-user c-id) => true
;2023-12-16T16:23:13.061Z NX-41294 DEBUG [leihs.procurement.resources.viewers:40] - (delete-viewers-for-category-id! tx c-id) => (1)
;2023-12-16T16:23:13.063Z NX-41294 DEBUG [leihs.procurement.resources.viewers:41] - (not (empty? u-ids)) => true
;2023-12-16T16:23:13.087Z NX-41294 DEBUG [leihs.procurement.resources.viewers:33] - (jdbc/execute! tx (-> (sql/insert-into :procurement_category_viewers) (sql/values row-maps) sql/format)) => (3)
;2023-12-16T16:23:13.087Z NX-41294 DEBUG [leihs.procurement.resources.viewers:42] - (insert-viewers! tx (map (fn* [p1__43117#] (hash-map :user_id p1__43117# :category_id c-id)) u-ids)) => (3)
;2023-12-16T16:23:13.088Z NX-41294 DEBUG [leihs.procurement.resources.categories:107] - (viewers/update-viewers! tx c-id (:viewers c)) => (3)
;2023-12-16T16:23:13.089Z NX-41294 DEBUG [leihs.procurement.resources.categories:110] - (user-perms/admin? tx auth-user) => false
;2023-12-16T16:23:13.090Z NX-41294 DEBUG [leihs.procurement.resources.categories:111] - (user-perms/inspector? tx auth-user c-id) => false