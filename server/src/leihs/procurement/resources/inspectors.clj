(ns leihs.procurement.resources.inspectors
  (:require
    ;[clojure.java.jdbc :as jdbc]
    ;        [leihs.procurement.utils.sql :as sql]

        [taoensso.timbre :refer [debug info warn error spy]]

        [leihs.core.utils :refer [my-cast]]


    [honey.sql :refer [format] :rename {format sql-format}]
    [leihs.core.db :as db]
    [next.jdbc :as jdbc]
    [honey.sql.helpers :as sql]

    [leihs.procurement.resources.users :refer [users-base-query]]
    ))


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

