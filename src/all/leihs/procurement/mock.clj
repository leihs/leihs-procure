(ns leihs.procurement.mock
  (:require [leihs.procurement.resources.user :as u]))

(def user-id "c0777d74-668b-5e01-abb5-f8277baa0ea8")

(defn wrap-set-authenticated-user
  [handler]
  (fn [request]
    (let [user-auth-entity (u/get-user-by-id (:tx request) user-id)]
      (handler (assoc request :authenticated-entity user-auth-entity)))))
