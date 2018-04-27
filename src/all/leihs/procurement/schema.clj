(ns leihs.procurement.schema
  (:require
    [clojure.java.io :as io]
    [clj-logging-config.log4j :as logging-config]
    [clojure.edn :as edn]
    [clojure.tools.logging :as logging]
    [com.walmartlabs.lacinia.util :as graphql-util]
    [com.walmartlabs.lacinia.resolve :as graphql-resolve]
    [com.walmartlabs.lacinia.schema :as graphql-schema]
    [leihs.procurement.env :as env]  
    [leihs.procurement.resources.admins :as admins]  
    [leihs.procurement.resources.attachments :as attachments]  
    [leihs.procurement.resources.budget-limits :as budget-limits]
    [leihs.procurement.resources.budget-period :as budget-period]
    [leihs.procurement.resources.budget-periods :as budget-periods]
    [leihs.procurement.resources.building :as building]
    [leihs.procurement.resources.category :as category]
    [leihs.procurement.resources.categories :as categories]
    [leihs.procurement.resources.current-user :as current-user]
    [leihs.procurement.resources.inspectors :as inspectors]
    [leihs.procurement.resources.main-category :as main-category]
    [leihs.procurement.resources.main-categories :as main-categories]
    [leihs.procurement.resources.model :as model]
    [leihs.procurement.resources.organization :as organization]
    [leihs.procurement.resources.organizations :as organizations]
    [leihs.procurement.resources.request :as request]
    [leihs.procurement.resources.request-fields :as request-fields]
    [leihs.procurement.resources.requests :as requests]
    [leihs.procurement.resources.requesters-organizations :as requesters-organizations]
    [leihs.procurement.resources.room :as room]
    [leihs.procurement.resources.rooms :as rooms]
    [leihs.procurement.resources.supplier :as supplier]
    [leihs.procurement.resources.user :as user]
    [leihs.procurement.resources.users :as users]
    [leihs.procurement.utils.ring-exception :refer [get-cause]]
    [logbug.debug :as debug]))

(defn wrap-resolver-with-error [resolver]
  (fn [context args value]
    (try
      (resolver context args value)
      (catch Throwable _e
        (let [e (get-cause _e)
              m (.getMessage e)]
          (logging/warn m)
          (if (= env/env :dev) (logging/debug e))
          (graphql-resolve/resolve-as value {:message m}))))))

; a function for debugging convenience. will be a var later.
(defn resolver-map []
  {:admins admins/get-admins
   :attachments attachments/get-attachments
   :budget-limits budget-limits/get-budget-limits
   :budget-period budget-period/get-budget-period
   :budget-periods budget-periods/get-budget-periods
   :building building/get-building
   :category category/get-category
   :categories categories/get-categories
   :current-user current-user/get-current-user
   :department organization/get-department
   :inspectors inspectors/get-inspectors
   :main-category main-category/get-main-category
   :main-categories main-categories/get-main-categories
   :model model/get-model
   :organization organization/get-organization
   :organizations organizations/get-organizations
   :priorities (fn [_ _ _] [0 1])
   :priorities-inspector (fn [_ _ _] [0 1 2 3])
   :request-by-id request/get-request
   :requests requests/get-requests
   :request-fields-by-id request-fields/get-request-fields
   :requesters-organizations requesters-organizations/get-requesters-organizations
   :room room/get-room
   :rooms rooms/get-rooms
   :supplier supplier/get-supplier
   :update-admins admins/update-admins
   :update-main-categories main-categories/update-main-categories!
   :update-requesters-organizations requesters-organizations/update-requesters-organizations
   :user user/get-user
   :users users/get-users})

(defn- wrap-map-with-error [arg]
  (into {} (for [[k v] arg] [k (wrap-resolver-with-error v)])))

(defn load-schema []
  (-> (io/resource "schema.edn")
      slurp
      edn/read-string
      (graphql-util/attach-resolvers (wrap-map-with-error (resolver-map)))
      graphql-schema/compile))

;#### debug ###################################################################
(logging-config/set-logger! :level :debug)
; (logging-config/set-logger! :level :info)
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
; (debug/undebug-ns *ns*)
