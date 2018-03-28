(ns leihs.procurement.permissions.request-field
  (:require [leihs.procurement.resources.user :as u]
            [leihs.procurement.resources.request :as r]
            [leihs.procurement.resources.category :as c]
            [leihs.procurement.resources.budget-period :as bp]))

(def user-id "c0777d74-668b-5e01-abb5-f8277baa0ea8")
(def request-id "91805c8c-0f47-45f1-bcce-b11da5427294")

(def test-user (u/get-user user-id))
(def test-request (r/get-request request-id))

(defn all-for-user-and-request [user, request]
  (let [budget-period (bp/get-budget-period (:budget_period_id request))
        category (c/get-category (:category_id request))
        request-without-template (not (:template_id request))
        requested-by-user (r/requested-by-user? request user)
        user-is-requester (u/procurement-requester? user)
        user-is-inspector (u/procurement-inspector? user)
        user-is-admin (u/procurement-admin? user)
        budget-period-is-past (bp/past? budget-period)
        budget-period-in-requesting-phase (bp/in-requesting-phase? budget-period)
        category-inspectable-by-user (c/inspectable-by? user category)]
    {:article_name {:read true,
                    :write (and request-without-template
                                (not budget-period-is-past)
                                (or (and user-is-requester requested-by-user budget-period-in-requesting-phase)
                                    category-inspectable-by-user 
                                    user-is-admin))}

     :article_number {:read true,
                      :write (and request-without-template
                                  (not budget-period-is-past)
                                  (or (and user-is-requester requested-by-user budget-period-in-requesting-phase)
                                      category-inspectable-by-user 
                                      user-is-admin))}

     :supplier {:read true,
                :write (and request-without-template
                            (not budget-period-is-past)
                            (or (and user-is-requester requested-by-user budget-period-in-requesting-phase)
                                category-inspectable-by-user 
                                user-is-admin))}

     :receiver {:read true,
                :write (and (not budget-period-is-past)
                            (or (and user-is-requester requested-by-user budget-period-in-requesting-phase)
                                category-inspectable-by-user 
                                user-is-admin))}

     :building_id {:read true,
                   :write (and (not budget-period-is-past)
                               (or (and user-is-requester requested-by-user budget-period-in-requesting-phase)
                                   category-inspectable-by-user 
                                   user-is-admin))}

     :room_id {:read true,
               :write (and (not budget-period-is-past)
                           (or (and user-is-requester requested-by-user budget-period-in-requesting-phase)
                               category-inspectable-by-user 
                               user-is-admin))}

     :motivation {:read true,
                  :write (and (not budget-period-is-past)
                              (or (and user-is-requester requested-by-user budget-period-in-requesting-phase)
                                  category-inspectable-by-user 
                                  user-is-admin))}

     :requested_quantity {:read true
                          :write (and (not budget-period-is-past)
                                      (or (and user-is-requester requested-by-user budget-period-in-requesting-phase)
                                          category-inspectable-by-user 
                                          user-is-admin))}

     :approved_quantity {:read (or (and user-is-requester requested-by-user budget-period-is-past)
                                   user-is-inspector
                                   user-is-admin)
                         :write (and (not budget-period-is-past)
                                     (or category-inspectable-by-user
                                         user-is-admin))}

     :order_quantity {:read (or user-is-inspector
                                user-is-admin)
                      :write (and (not budget-period-is-past)
                                  (or category-inspectable-by-user
                                      user-is-admin))}

     :inspection_comment {:read (or (and user-is-requester requested-by-user budget-period-is-past)
                                    user-is-inspector
                                    user-is-admin)
                          :write (and (not budget-period-is-past)
                                      (or category-inspectable-by-user
                                          user-is-admin))}
     
     :attachments {:read true
                   :write (and (not budget-period-is-past)
                               (or (and user-is-requester requested-by-user budget-period-in-requesting-phase)
                                   category-inspectable-by-user 
                                   user-is-admin))}

     :accounting_type {:read user-is-inspector
                       :write (and (not budget-period-is-past)
                                   (or category-inspectable-by-user
                                       user-is-admin))}

     :internal_order_number {:read user-is-inspector
                             :write (and (not budget-period-is-past)
                                         (or category-inspectable-by-user
                                             user-is-admin))}

     :state {:read true
             :write (and (not budget-period-is-past)
                         (or category-inspectable-by-user
                             user-is-admin))}
     }))

; (into {}
;       (filter #(:read (second %))
;               (seq (all-for-user-and-request test-user test-request))))
