(ns leihs.procurement.permissions.request-field
  (:require [leihs.procurement.resources.user :as u]
            [leihs.procurement.resources.request :as r]
            [leihs.procurement.resources.category :as c]
            [leihs.procurement.resources.budget-period :as bp]
            [leihs.procurement.permissions.request :as r-perm]))

(def user-id "c0777d74-668b-5e01-abb5-f8277baa0ea8")
(def request-id "91805c8c-0f47-45f1-bcce-b11da5427294")

(def test-user (u/get-user user-id))
(def test-request (r/get-request request-id))

(defn all-for-user-and-request [user, request]
  (let [budget-period (bp/get-budget-period (:budget_period_id request))
        category (c/get-category (:category_id request))
        request-editable-by-user (r-perm/edit? user request)
        request-without-template (not (:template_id request))
        requested-by-user (r/requested-by-user? request user)
        user-is-requester (u/procurement-requester? user)
        user-is-inspector (u/procurement-inspector? user)
        user-is-admin (u/procurement-admin? user)
        budget-period-is-past (bp/past? budget-period)
        category-inspectable-by-user (c/inspectable-by? user category)]
    {:article_name {:read true,
                    :write (and request-editable-by-user
                                request-without-template)}

     :requested_quantity {:read true
                          :write request-editable-by-user}

     :approved_quantity {:read (or (and user-is-requester
                                        requested-by-user)
                                   user-is-inspector
                                   user-is-admin)
                         :write (and (not budget-period-is-past)
                                     category-inspectable-by-user)}

     :inspection_comment {:read (or (and user-is-requester
                                         requested-by-user
                                         budget-period-is-past)
                                    user-is-inspector
                                    user-is-admin)
                          :write (and (not budget-period-is-past)
                                      category-inspectable-by-user)}}))

(all-for-user-and-request test-user test-request)
