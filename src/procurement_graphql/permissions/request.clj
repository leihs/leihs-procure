(ns procurement-graphql.permissions.request
  (:require [procurement-graphql.resources.request :as request-res]
            [procurement-graphql.resources.budget-period :as budget-period-res]
            [procurement-graphql.resources.user :as user-res]))  

(def user-id "c0777d74-668b-5e01-abb5-f8277baa0ea8")
(def request-id "91805c8c-0f47-45f1-bcce-b11da5427294")

(defn edit? [id user-id]
  (let [request (request-res/get-request id)
        budget_period (budget-period-res/get-budget-period (:budget_period_id request))
        user (user-res/get-user user-id)]
    (and (user-res/procurement-requester? user-id)
         (= (:user-id request) user-id)
         ; (budget-period/in-requesting-phase?)
         )))

(edit? request-id user-id)
