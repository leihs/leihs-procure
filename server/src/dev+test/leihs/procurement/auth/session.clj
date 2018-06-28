(ns leihs.procurement.auth.session
  (:require [clojure.tools.logging :as log]
            [leihs.procurement.resources.user :as u]))

(defn wrap
  [handler]
  (fn [request]
    (let [user-id (or (-> request
                          :query-params
                          :user_id)
                      (-> request
                          :headers
                          (get "x-fake-token-authorization")))
          user (u/get-user-by-id (:tx request) user-id)]
      (handler (assoc request :authenticated-entity {:user_id (:id user)})))))
