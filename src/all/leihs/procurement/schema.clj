(ns leihs.procurement.schema
  (:require
    [clojure.java.io :as io]
    [clojure.java.jdbc :as jdbc]
    [com.walmartlabs.lacinia.util :as util]
    [com.walmartlabs.lacinia.schema :as schema]
    [clojure.edn :as edn]
    [leihs.procurement.db :as db]
    [leihs.procurement.resources.request :as r]
    ))

(defn get-request [context arguments value]
  (let [{:keys [id]} arguments]
    (r/get-request id)))

(def resolver-map {:request-by-id get-request})

(defn load-schema []
  (-> (io/resource "schema.edn")
      slurp
      edn/read-string
      (util/attach-resolvers resolver-map)
      schema/compile))
