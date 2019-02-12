(ns leihs.procurement.auth.session
  (:require
    [leihs.procurement.resources.user :as user]))

(defn wrap
  [handler]
  (fn [request]
    (let [db-user
            (some->
              request
              :headers
              (get "x-fake-token-authorization")
              (->>
                (user/get-user-by-id (:tx request))))]
      (handler
        (cond-> request
          db-user
            (assoc :authenticated-entity
              (clojure.set/rename-keys db-user {:id :user_id})))))))
