(ns leihs.procurement.auth.session
  (:require [leihs.procurement.resources.user :as u]))

(defn wrap
  [handler]
  (fn [request]
    ; FIXME: query params does not work
    (let [user-id (or (-> request
                          :query-params
                          :user_id)
                      (-> request
                          :headers
                          (get "x-fake-token-authorization")))
          user (u/get-user-by-id (:tx request) user-id)]
      (handler (assoc request :authenticated-entity {:user_id (:id user)})))))
