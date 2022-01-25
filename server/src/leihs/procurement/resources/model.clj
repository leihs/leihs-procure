(ns leihs.procurement.resources.model
  (:require [clojure.java.jdbc :as jdbc]
            [leihs.procurement.utils.sql :as sql]))

(defn model-query
  [id]
  (-> (sql/select :models.*)
      (sql/from :models)
      (sql/where [:= :models.id id])
      sql/format))

(defn get-model-by-id [tx id] (first (jdbc/query tx (model-query id))))

(defn get-model
  [context _ value]
  (get-model-by-id (-> context
                       :request
                       :tx)
                   (or (:value value) ; for RequestFieldModel
                       (:model_id value))))
