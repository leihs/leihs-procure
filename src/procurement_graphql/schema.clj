(ns procurement-graphql.schema
  (:require
    [clojure.java.io :as io]
    [clojure.java.jdbc :as jdbc]
    [com.walmartlabs.lacinia.util :as util]
    [com.walmartlabs.lacinia.schema :as schema]
    [clojure.edn :as edn]
    [procurement-graphql.db :as db]
    [procurement-graphql.resources.request :as request]
    ))

(defn get-request [context arguments value]
  (let [{:keys [id]} arguments]
    (first (jdbc/query db/db (request/request-query id)))))

(defn resolver-map []
  {:request-by-id get-request})

(defn load-schema []
  (-> (io/resource "schema.edn")
      slurp
      edn/read-string
      (util/attach-resolvers (resolver-map))
      schema/compile))

; (require '[procurement-graphql.schema :reload-all true])
