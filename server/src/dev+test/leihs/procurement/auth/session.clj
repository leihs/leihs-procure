(ns leihs.procurement.auth.session
  (:require [leihs.procurement.resources.user :as u]))

(defn wrap
  [handler]
  (fn [request]
    (let [user-id (some-> request
                          :headers
                          (get "x-fake-token-authorization")
                          (->> (u/get-user-by-id (:tx request)))
                          :id)]
      (handler (assoc request :authenticated-entity {:user_id user-id})))))
