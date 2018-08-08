(ns leihs.procurement.resources.current-user
  (:require [leihs.procurement.resources [saved-filters :as saved-filters]
             [user :as user]]))

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
