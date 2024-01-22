(ns leihs.procurement.resources.category
  (:require
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.procurement.utils.helpers :refer [to-uuid]]
   [leihs.procurement.utils.sql :as sqlp]
   [next.jdbc :as jdbc]
   [taoensso.timbre :refer [debug error info spy warn]]))

(def category-base-query
  (-> (sql/select :procurement_categories.*)
      (sql/from :procurement_categories)))

(defn category-query
  [id]
  (-> category-base-query
      (sql/where [:= :procurement_categories.id (to-uuid id)])
      sql-format))

(defn get-category
  ([context _ value]
   (jdbc/execute-one! (-> context
                          :request
                          :tx-next)
                      (category-query (or (:value value)
                                          ; for
                                          ; RequestFieldCategory
                                          (:category_id value)))))
  ([tx catmap]
   (let [where-clause (sqlp/map->where-clause :procurement_categories catmap)]
     (jdbc/execute-one! tx
                        (-> category-base-query
                            (sql/where where-clause)
                            sql-format)))))

(defn get-category-by-id
  [tx id]
  (->> id
       category-query
       (jdbc/execute-one! tx)))

(defn can-delete?
  [context _ value]
  (-> (jdbc/execute-one!
       (-> context
           :request
           :tx-next) (-> [:and
                          [:not
                           [:exists
                            (-> (sql/select true)
                                (sql/from [:procurement_requests :pr])
                                (sql/where [:= :pr.category_id (:id value)]))]]
                          [:not
                           [:exists
                            (-> (sql/select true)
                                (sql/from [:procurement_templates :pt])
                                (sql/where [:= :pt.category_id (:id value)]))]]]
                         (vector :result)
                         sql/select
                         sql-format))
      :result))

(defn update-category!
  [tx c]
  (jdbc/execute! tx
                 (-> (sql/update :procurement_categories)
                     (sql/set c)
                     (sql/where [:= :procurement_categories.id (:id c)])
                     sql-format)))

(defn insert-category!
  [tx c]
  (jdbc/execute! tx
                 (-> (sql/insert-into :procurement_categories)
                     (sql/values [c])
                     sql-format)))
