(ns leihs.procurement.auth.session
  (:require [leihs.procurement.resources.user :as user]))

(defn wrap
  [handler]
  (fn [request]
    (let [user-id (some-> request
                          :headers
                          (get "x-fake-token-authorization")
                          (->> (user/get-user-by-id (:tx request)))
                          :id)]
      (handler (cond-> request
                 user-id (assoc :authenticated-entity {:user_id user-id}))))))
