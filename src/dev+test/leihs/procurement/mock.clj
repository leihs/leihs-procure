(ns leihs.procurement.mock
  (:require [clojure.tools.logging :as log]
            [leihs.procurement.resources.user :as u]))

(defn wrap-set-authenticated-user
  [handler]
  (fn [request]
    (let [user-auth-entity (u/get-user-by-id (:tx request)
                                             (-> request
                                                 :headers
                                                 :Authorization))]
      (handler (assoc request :authenticated-entity user-auth-entity)))))
