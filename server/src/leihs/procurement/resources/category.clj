(ns leihs.procurement.resources.category
  (:require 
    ;[clojure.java.jdbc :as jdbc]
            [leihs.procurement.utils.sql :as sqlp]

    [honey.sql :refer [format] :rename {format sql-format}]
    [leihs.core.db :as db]
    [next.jdbc :as jdbc]
    [honey.sql.helpers :as sql]
    ))

(def category-base-query
  (-> (sql/select :procurement_categories.*)
      (sql/from :procurement_categories)))

(defn category-query
  [id]
  (-> category-base-query
      ;(sql/where [:= :procurement_categories.id id])
      (sql/where [:= :procurement_categories.id [:cast id :uuid]])
      sql-format))

(defn get-category
  ([context _ value]
   (jdbc/execute-one! (-> context
                          :request
                          :tx-next)
                      (category-query (or (:value value)
                                          ; for
                                          ; RequestFieldCategory
                                          [:cast (:category_id value) :uuid]))))


  ([tx catmap]
   (let [where-clause (sqlp/map->where-clause :procurement_categories catmap)]
     ((jdbc/execute-one! tx
                        (-> category-base-query
                            (sql/where where-clause)
                            sql-format))))))

(defn get-category-by-id
  [tx id]
  (->> id
       category-query
       (jdbc/execute-one! tx)
       ))

(defn can-delete?
  [context _ value]
  (println ">> can-delete1")
  (-> (jdbc/execute-one!
        (-> context
            :request
            :tx-next)
        (-> (
              :and
              ( :not
                        ( :exists
                                  (-> (sql/select true)
                                      (sql/from [:procurement_requests :pr])
                                      (sql/where [:= :pr.category_id
                                                        [:cast(:id value):uuid]]))))
              ( :not
                        ( :exists
                                  (-> (sql/select true)
                                      (sql/from [:procurement_templates :pt])
                                      (sql/where [:= :pt.category_id
                                                        [:cast (:id value) :uuid]])))))
            (vector :result)
            sql/select
            sql-format))
      :result))

(defn update-category!
  [tx c]
  (jdbc/execute! tx
                 (-> (sql/update :procurement_categories)
                     (sql/set c)
                     (sql/where [:= :procurement_categories.id [:cast (:id c) :uuid]])
                     sql-format)))

(defn insert-category!
  [tx c]
  (jdbc/execute! tx
                 (-> (sql/insert-into :procurement_categories)
                     (sql/values [c])
                     sql-format)))
