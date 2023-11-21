(ns leihs.procurement.resources.model
  (:require

    [honey.sql :refer [format] :rename {format sql-format}]
    [leihs.core.db :as db]
    [next.jdbc :as jdbc]
    [honey.sql.helpers :as sql]
    
    ;[clojure.java.jdbc :as jdbc]
    ;        [leihs.procurement.utils.sql :as sql]
    
    
    ))

(defn model-query
  [id]
  (-> (sql/select :models.*)
      (sql/from :models)
      (sql/where [:= :models.id id])
      sql-format))

(defn get-model-by-id [tx id] ((jdbc/execute-one! tx (model-query id))))

(defn get-model
  [context _ value]
  (get-model-by-id (-> context
                       :request
                       :tx)
                   (or (:value value) ; for RequestFieldModel
                       (:model_id value))))
