(ns leihs.procurement.mock
  (:require [clojure.tools.logging :as log]
            [leihs.procurement.resources.user :as u]))

(defn wrap-set-authenticated-user
  [handler]
  (fn [request]
    (let [user-id (or (-> request
                          :query-params
                          :user_id)
                      (-> request
                          :headers
                          :Authorization))
          user-auth-entity (u/get-user-by-id (:tx request) (log/spy user-id))]
      (handler (assoc request :authenticated-entity user-auth-entity)))))
