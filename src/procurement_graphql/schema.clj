(ns procurement-graphql.schema
  "Contains custom resolvers and a function to provide the full schema."
  (:require
    [clojure.java.io :as io]
    [clojure.java.jdbc :as jdbc]
    [com.walmartlabs.lacinia.util :as util]
    [com.walmartlabs.lacinia.schema :as schema]
    [clojure.edn :as edn]
    [honeysql.core :as sql]
    [honeysql.helpers :refer :all :as helpers]
    [procurement-graphql.db :as db]
    ))

(defn request-query [id]
  (-> (select :*)
      (from :procurement_requests)
      (where [:= :procurement_requests.id (sql/call :cast id :uuid)])
      sql/format))

(defn get-request [context arguments value]
  (let [{:keys [id]} arguments]
    (first (jdbc/query db/db (request-query id)))))

(defn resolver-map []
  {:request-by-id get-request})

(defn load-schema []
  (-> (io/resource "schema.edn")
      slurp
      edn/read-string
      (util/attach-resolvers (resolver-map))
      schema/compile))

; (require '[procurement-graphql.schema :reload-all true])

(first (jdbc/query db/db (request-query "91805c8c-0f47-45f1-bcce-b11da5427294")))
