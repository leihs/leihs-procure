(ns leihs.procurement.schema
  (:require
    [clojure.java.io :as io]
    [clj-logging-config.log4j :as logging-config]
    [clojure.edn :as edn]
    [clojure.tools.logging :as logging]
    [com.walmartlabs.lacinia.util :as util]
    [com.walmartlabs.lacinia.schema :as schema]
    [leihs.procurement.resources.attachments :as attachments]  
    [leihs.procurement.resources.budget-limits :as budget-limits]
    [leihs.procurement.resources.budget-period :as budget-period]
    [leihs.procurement.resources.budget-periods :as budget-periods]
    [leihs.procurement.resources.building :as building]
    [leihs.procurement.resources.category :as category]
    [leihs.procurement.resources.categories :as categories]
    [leihs.procurement.resources.main-category :as main-category]
    [leihs.procurement.resources.main-categories :as main-categories]
    [leihs.procurement.resources.model :as model]
    [leihs.procurement.resources.organizations :as organizations]
    [leihs.procurement.resources.request :as request]
    [leihs.procurement.resources.request-fields :as request-fields]
    [leihs.procurement.resources.requests :as requests]
    [leihs.procurement.resources.room :as room]
    [leihs.procurement.resources.rooms :as rooms]
    [leihs.procurement.resources.supplier :as supplier]
    [leihs.procurement.resources.user :as user]
    [logbug.debug :as debug]))

(defn resolver-map []
  {:attachments attachments/get-attachments
   :budget_limits budget-limits/get-budget-limits
   :budget_period budget-period/get-budget-period
   :budget_periods budget-periods/get-budget-periods
   :building building/get-building
   :category category/get-category
   :categories categories/get-categories
   :main_category main-category/get-main-category
   :main_categories main-categories/get-main-categories
   :model model/get-model
   :organizations organizations/get-organizations
   :request-by-id request/get-request
   :requests requests/get-requests
   :request-fields-by-id request-fields/get-request-fields
   :room room/get-room
   :rooms rooms/get-rooms
   :supplier supplier/get-supplier
   :user user/get-user})

(defn load-schema []
  (-> (io/resource "schema.edn")
      slurp
      edn/read-string
      (util/attach-resolvers (resolver-map))
      schema/compile))

;#### debug ###################################################################
; (logging-config/set-logger! :level :debug)
; (logging-config/set-logger! :level :info)
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
; (debug/undebug-ns *ns*)
