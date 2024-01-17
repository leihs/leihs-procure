(ns leihs.procurement.graphql
  (:require
   [clojure.edn :as edn]
   (clojure.java [io :as io])
   (clojure.java [io :as io])
    ;; all needed imports
   [com.walmartlabs.lacinia :as lacinia]
   (com.walmartlabs.lacinia [schema :as graphql-schema] [util :as graphql-util])
   [leihs.core.graphql :as core-graphql]
   [leihs.procurement.graphql.resolver :as resolver]
   [leihs.procurement.graphql.scalars :as scalars]
   [taoensso.timbre :refer [debug error info spy warn]]))

(defn load-schema! []
  (or (some-> (io/resource "schema.edn")
              slurp edn/read-string
              (graphql-util/attach-resolvers resolver/resolvers)
              (graphql-util/attach-scalar-transformers scalars/scalars)
              graphql-schema/compile)
      (throw (ex-info "Failed to load schema" {}))))

(defn exec-query
  [query-string request]
  (debug "graphql query" query-string
         "with variables" (-> request
                              :body
                              :variables))
  (lacinia/execute (core-graphql/schema)
                   query-string
                   (-> request
                       :body
                       :variables)
                   {:request request}))

(defn handler
  [{{query :query} :body, :as request}]
  (let [result (exec-query query request)
        resp {:body result}]
    (if (:errors result)
      (do (debug result) (assoc resp :graphql-error true))
      resp)))

(defn init []
  (core-graphql/init-schema! (load-schema!)))
