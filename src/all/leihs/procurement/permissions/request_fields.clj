(ns leihs.procurement.permissions.request-fields
  (:require [leihs.procurement.resources.user :as user]
            [leihs.procurement.resources.request :as request]
            [leihs.procurement.resources.category :as category]
            [leihs.procurement.resources.budget-period :as budget-period]))

(defn all-for-user-and-request [context _ _]
  (let [proc-request (:proc-request context)
        request (:request context)
        tx (:tx request)
        user (user/get-user-by-id tx (-> request :authenticated-entity :id))
        budget-period (budget-period/get-budget-period-by-id tx (:budget_period_id proc-request))
        category (category/get-category-by-id tx (:category_id proc-request))
        request-without-template (not (:template_id proc-request))
        requested-by-user (= (:user_id proc-request) (:id user))
        user-is-requester (:is_procurement_requester user)
        user-is-inspector (user/inspector? tx user)
        user-is-admin (:is_procurement_admin user)
        budget-period-is-past (budget-period/past? tx budget-period)
        budget-period-in-requesting-phase (budget-period/in-requesting-phase? tx budget-period)
        category-inspectable-by-user (category/inspectable-by? tx user category)]
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
     }))
