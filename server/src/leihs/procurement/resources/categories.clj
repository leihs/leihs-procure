(ns leihs.procurement.resources.categories
  (:require
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.procurement.authorization :as authorization]
   [leihs.procurement.permissions.user :as user-perms]
   (leihs.procurement.resources [category :as category]
                                [inspectors :as inspectors] [viewers :as viewers])
   [next.jdbc :as jdbc]
   [taoensso.timbre :refer [debug error info spy warn]]))

(def categories-base-query
  (-> (sql/select :procurement_categories.*)
      (sql/from :procurement_categories)
      (sql/order-by [:procurement_categories.name :asc])))

(defn categories-query
  [context arguments value]
  (let [id (:id arguments)
        inspected-by-auth-user (:inspected_by_auth_user arguments)
        main-category-id (:id value)]
    (sql-format
     (cond-> categories-base-query
       id (sql/where [:in :procurement_categories.id id])
       main-category-id (sql/where
                         [:= :procurement_categories.main_category_id
                          main-category-id])
       inspected-by-auth-user
       (-> (sql/join :procurement_category_inspectors
                     [:= :procurement_category_inspectors.category_id
                      :procurement_categories.id])
           (sql/where [:= :procurement_category_inspectors.user_id
                       (-> context
                           :request
                           :authenticated-entity
                           :user_id)]))))))

(defn get-categories-for-ids
  [tx ids]
  (if (empty? ids)
    []
    (jdbc/execute! tx (-> categories-base-query
                          (sql/where [:in :procurement_categories.id ids])
                          sql-format))))

(defn get-for-main-category-id
  [tx main-cat-id]
  (jdbc/execute! tx (-> categories-base-query
                        (sql/where [:= :procurement_categories.main_category_id main-cat-id])
                        sql-format)))

(defn get-categories
  [context arguments value]
  (if (= (:id arguments) [])
    []
    (->> (categories-query context arguments value)
         (jdbc/execute! (-> context
                            :request
                            :tx)))))

(defn delete-categories-for-main-category-id-and-not-in-ids!
  [tx mc-id ids]
  (jdbc/execute! tx (-> (sql/delete-from :procurement_categories)
                        (sql/where [:= :procurement_categories.main_category_id mc-id])
                        (cond-> (not (empty? ids)) (sql/where [:not-in :procurement_categories.id ids]))
                        sql-format)))

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
        (jdbc/execute! tx (-> categories-base-query
                              (sql/where [:in :procurement_categories.id (map :id categories)])
                              sql-format))))))
