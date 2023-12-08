(ns leihs.procurement.resources.model
  (:require [clojure.java.jdbc :as jdbc]

                [taoensso.timbre :refer [debug info warn error spy]]


            [leihs.procurement.utils.sql :as sql]))














(defn model-query
  [id]
  (-> (sql/select :models.*)
      (sql/from :models)
      (sql/where [:= :models.id (spy id)])
      sql/format))

(defn get-model-by-id [tx id] (first (jdbc/query tx (spy (model-query id)))))

(defn get-model
  [context _ value]
  (get-model-by-id (-> context
                       :request
                       :tx)
                   (or (spy (:value value) )                      ; for RequestFieldModel
                       (spy (:model_id value)))))
