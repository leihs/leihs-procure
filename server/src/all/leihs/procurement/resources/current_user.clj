(ns leihs.procurement.resources.current-user
  (:require [clj-logging-config.log4j :as logging-config]
            [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as logging]
            [leihs.procurement.resources.saved-filters :as saved-filters]
            [leihs.procurement.resources.user :as user]
            [leihs.procurement.utils.sql :as sql]
            [leihs.procurement.utils.ds :refer [get-ds]]
            [logbug.debug :as debug]))

(defn get-current-user
  [{request :request} _ _]
  (let [tx (:tx request)
        user-id (-> request
                    :authenticated-entity
                    :user_id)
        user (user/get-user-by-id tx user-id)
        saved-filters (saved-filters/get-saved-filters-by-user-id tx user-id)]
    {:user user, :saved_filters (:filter saved-filters)}))

;#### debug ###################################################################
; (logging-config/set-logger! :level :debug)
; (logging-config/set-logger! :level :info)
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
; (debug/undebug-ns *ns*)
