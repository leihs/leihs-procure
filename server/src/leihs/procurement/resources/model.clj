(ns leihs.procurement.resources.model
  (:require
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [next.jdbc :as jdbc]
   [taoensso.timbre :refer [debug error info spy warn]]))

(defn model-query
  [id]
  (-> (sql/select :models.*)
      (sql/from :models)
      (sql/where [:= :models.id id])
      sql-format))

(defn get-model-by-id [tx id] (jdbc/execute-one! tx (model-query id)))

(defn get-model
  [context _ value]
  (get-model-by-id (-> context
                       :request
                       :tx-next)
                   (or (:value value) ; for RequestFieldModel
                       (:model_id value))))
