(ns leihs.procurement.schema
  (:require
    [clojure.java.io :as io]
    [clj-logging-config.log4j :as logging-config]
    [clojure.edn :as edn]
    [clojure.tools.logging :as logging]
    [com.walmartlabs.lacinia.util :as util]
    [com.walmartlabs.lacinia.schema :as schema]
    [leihs.procurement.permissions.request-field :as rf-perms]  
    [leihs.procurement.resources.attachments :as attachments]  
    [leihs.procurement.resources.budget-periods :as budget-periods]
    [leihs.procurement.resources.building :as building]
    [leihs.procurement.resources.category :as category]
    [leihs.procurement.resources.categories :as categories]
    [leihs.procurement.resources.main_categories :as main-categories]
    [leihs.procurement.resources.model :as model]
    [leihs.procurement.resources.organizations :as organizations]
    [leihs.procurement.resources.request :as proc-request]
    [leihs.procurement.resources.requests :as proc-requests]
    [leihs.procurement.resources.room :as room]
    [leihs.procurement.resources.supplier :as supplier]
    [leihs.procurement.resources.user :as user]
    [logbug.debug :as debug]))

(defn get-attachments [{request :request} _ {request_id :id}]
  (attachments/get-attachments request request_id))

(defn get-budget-periods [context arguments _]
  (budget-periods/get-budget-periods context arguments))

(defn get-building [{request :request} _ {id :building_id}]
  (building/get-building request id))

(defn get-category [{request :request} _ {id :category_id}]
  (category/get-category request id))

(defn get-categories [context arguments _]
  (categories/get-categories context arguments))

(defn get-main-categories [context arguments _]
  (main-categories/get-main-categories context arguments))

(defn get-model [{request :request} _ {id :model_id}]
  (model/get-model request id))

(defn get-organizations [context arguments _]
  (organizations/get-organizations context arguments))

(defn get-request [{request :request} arguments _]
  (proc-request/get-request request arguments))

(defn get-requests [context arguments _]
  (proc-requests/get-requests context arguments))

(defn get-room [{request :request} _ {id :room_id}]
  (room/get-room request id))

(defn get-request-fields [context arguments _]
  (let [request (proc-request/get-request {:request context} arguments)
        rf-perms (rf-perms/all-for-user-and-request
                   (assoc context :proc-request request))]
    (map (fn [[k v]] (merge v {:name k, :value (k request)}))
         (seq rf-perms))))

(defn get-supplier [{request :request} _ {id :supplier_id}]
  (supplier/get-supplier request id))

(defn get-user [{request :request} _ {id :user_id}]
  (user/get-user request id))

(defn resolver-map []
  {:attachments get-attachments
   :budget_periods get-budget-periods
   :building get-building
   :category get-category
   :categories get-categories
   :main_categories get-main-categories
   :model get-model
   :organizations get-organizations
   :request-by-id get-request
   :requests get-requests
   :request-fields-by-id get-request-fields
   :room get-room
   :supplier get-supplier
   :user get-user})

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
