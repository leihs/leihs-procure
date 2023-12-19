(ns leihs.procurement.resources.supplier
  (:require

    ;[clojure.java.jdbc :as jdbc]
    ;        [leihs.procurement.utils.sql :as sql]


    [taoensso.timbre :refer [debug info warn error spy]]


    [honey.sql :refer [format] :rename {format sql-format}]
    [leihs.core.db :as db]
    [next.jdbc :as jdbc]
    [honey.sql.helpers :as sql]

    ))

(defn supplier-query
  [id]
  (println ">o> tocheck suppliers.id=" id)
  (-> (sql/select :suppliers.*)
      (sql/from :suppliers)
      (sql/where [:= :suppliers.id [:cast (spy id) :uuid]])
      ;(sql/where [:= :suppliers.id (spy id) ])
      sql-format
      spy
      ))

(defn get-supplier-by-id [tx id]
  (jdbc/execute-one! tx (supplier-query id)))

(defn get-supplier
  [context _ value]


  (spy (-> context
           :request
           :tx-next))

  (spy (get-supplier-by-id (spy (-> context
                                    :request
                                    :tx-next))
                           (or (spy (:value (spy value)))         ; for RequestFieldSupplier
                               (spy (:supplier_id (spy value))))
                           ))
  )
