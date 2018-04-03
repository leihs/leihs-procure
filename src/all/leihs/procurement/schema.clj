(ns leihs.procurement.schema
  (:require
    [clojure.java.io :as io]
    [clj-logging-config.log4j :as logging-config]
    [clojure.edn :as edn]
    [clojure.tools.logging :as logging]
    [com.walmartlabs.lacinia.util :as util]
    [com.walmartlabs.lacinia.schema :as schema]
    [leihs.procurement.permissions.request-field :as rf-perms]  
    [leihs.procurement.resources.budget-periods :as bps]
    [leihs.procurement.resources.category :as c]
    [leihs.procurement.resources.categories :as cs]
    [leihs.procurement.resources.organizations :as os]
    [leihs.procurement.resources.request :as r]
    [leihs.procurement.resources.requests :as rs]
    [logbug.debug :as debug]))

(defn get-budget-periods [context arguments _]
  (bps/get-budget-periods context arguments))

(defn get-category [{request :request} _ {id :category_id}]
  (c/get-category request id))

(defn get-categories [context arguments _]
  (cs/get-categories context arguments))

(defn get-organizations [context arguments _]
  (os/get-organizations context arguments))

(defn get-request [context arguments _]
  (let [{:keys [id]} arguments]
    (r/get-request (:request context) id)))

(defn get-requests [context arguments _]
  (rs/get-requests context arguments))

(defn get-request-fields [context arguments _]
  (let [request (r/get-request context arguments)
        rf-perms (rf-perms/all-for-user-and-request
                   (assoc context :proc-request request))]
    (map (fn [[k v]] (merge v {:name k, :value (k request)}))
         (seq rf-perms))))

(defn resolver-map []
  {:budget_periods get-budget-periods
   :category get-category
   :categories get-categories
   :organizations get-organizations
   :request-by-id get-request
   :requests get-requests
   :request-fields-by-id get-request-fields})

(defn load-schema []
  (-> (io/resource "schema.edn")
      slurp
      edn/read-string
      (util/attach-resolvers (resolver-map))
      schema/compile))

;#### debug ###################################################################
(logging-config/set-logger! :level :debug)
; (logging-config/set-logger! :level :info)
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
; (debug/undebug-ns *ns*)
