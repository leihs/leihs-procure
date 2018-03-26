(ns procurement-graphql.schema
  "Contains custom resolvers and a function to provide the full schema."
  (:require
    [clojure.java.io :as io]
    [clojure.java.jdbc :as jdbc]
    [com.walmartlabs.lacinia.util :as util]
    [com.walmartlabs.lacinia.schema :as schema]
    [clojure.edn :as edn]
    [procurement-graphql.db :as db]
    ))

(defn get-request [context arguments value]
  (let [{:keys [id]} arguments]
    (first
      (jdbc/query db/db
                  [(str "SELECT * FROM procurement_requests WHERE id = " id)]))))

(defn resolver-map []
  {:request-by-id get-request})

(defn load-schema []
  (-> (io/resource "schema.edn")
      slurp
      edn/read-string
      (util/attach-resolvers (resolver-map))
      schema/compile))

; (require '([procurement-graphql.schema :reload-all]))
; (first
;   (jdbc/query db
;               [(str "SELECT * FROM procurement_requests WHERE id = " "'91805c8c-0f47-45f1-bcce-b11da5427294'")]))
