(ns leihs.procurement.resources.model
  (:require

    [honey.sql :refer [format] :rename {format sql-format}]
    [leihs.core.db :as db]
    [next.jdbc :as jdbc]
    [honey.sql.helpers :as sql]

        [taoensso.timbre :refer [debug info warn error spy]]


    ;[clojure.java.jdbc :as jdbc]
    ;        [leihs.procurement.utils.sql :as sql]

    ))

(defn model-query
  [id]
  (-> (sql/select :models.*)
      (sql/from :models)
      (sql/where [:= :models.id [:cast (spy id) :uuid]])
      sql-format))

(defn get-model-by-id [tx id]
  (println "\n>o> NPE?? model::get-model-by-id id" id)
  (spy (jdbc/execute-one! tx (spy (model-query id)))))

(defn get-model
  [context _ value]
  (get-model-by-id (-> context
                       :request
                       :tx-next)
                   (or (spy (:value value) )                      ; for RequestFieldModel
                       (spy (:model_id value)))))
