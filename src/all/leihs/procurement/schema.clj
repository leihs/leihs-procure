(ns leihs.procurement.schema
  (:require
    [clojure.java.io :as io]
    [clj-logging-config.log4j :as logging-config]
    [clojure.tools.logging :as logging]
    [com.walmartlabs.lacinia.util :as util]
    [com.walmartlabs.lacinia.schema :as schema]
    [clojure.edn :as edn]
    [leihs.procurement.resources.request :as r]))

(defn get-request [context arguments value]
  (let [{:keys [id]} arguments]
    (r/get-request context id)))

(defn get-requests [context arguments value]
  (let [{requested-by-auth-user :requested_by_auth_user} arguments]
    (logging/debug requested-by-auth-user)
    (if requested-by-auth-user
      (r/get-requests-by-requester context)
      (r/get-requests-not-by-requester context))))

(def resolver-map {:request-by-id get-request
                   :requests get-requests})

(defn load-schema []
  (-> (io/resource "schema.edn")
      slurp
      edn/read-string
      (util/attach-resolvers resolver-map)
      schema/compile))

;#### debug ###################################################################
(logging-config/set-logger! :level :debug)
; (logging-config/set-logger! :level :info)
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
; (debug/undebug-ns *ns*)
