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
  (let [request (:request context)
        tx (:tx request)
        auth-user (:authenticated-entity request)
        categories (:input_data args)]
    (loop [[c & rest-cs] categories]
      (if-let [c-id (:id c)]
        (do (authorization/authorize-and-apply
              #(viewers/update-viewers! tx c-id (:viewers c))
              :if-any
              [#(user-perms/admin? tx auth-user)
               #(user-perms/inspector? tx auth-user c-id)])
            (recur rest-cs))
        (jdbc/query tx
                    (-> categories-base-query
                        (sql/merge-where [:in :procurement_categories.id
                                          (map :id categories)])
                        sql/format))))))

