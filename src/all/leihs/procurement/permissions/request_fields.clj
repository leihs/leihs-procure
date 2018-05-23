(ns leihs.procurement.permissions.request-fields
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log]
            [leihs.procurement.resources.budget-period :as budget-period]
            [leihs.procurement.permissions.user :as user-perms]
            [leihs.procurement.utils.ds :as ds]))

;; ==================================================================
;; TODO: this is temporary. The r/w permissions for theses attributes
;; need to be revisited and defined.
(def field-exceptions
  #{:budget_period_id :category_id :model_id :organization_id :room_id
    :user_id})
;; ==================================================================

(defn get-for-user-and-request
  [tx user proc-request]
  (let [budget-period (budget-period/get-budget-period-by-id tx
                                                             (:budget_period_id
                                                               proc-request))
        request-without-template (not (:template_id proc-request))
        requested-by-user (user-perms/requester-of? tx user proc-request)
        user-is-requester (user-perms/requester? tx user)
        user-is-inspector (user-perms/inspector? tx user)
        user-is-admin (user-perms/admin? tx user)
        budget-period-is-past (budget-period/past? tx budget-period)
        budget-period-in-requesting-phase
          (budget-period/in-requesting-phase? tx budget-period)
        category-inspectable-by-user
          (user-perms/inspector? tx user (:category_id proc-request))]
    {:id {:read true, :write false},
     :article_name {:read true,
                    :write (and request-without-template
                                (not budget-period-is-past)
                                (or (and user-is-requester
                                         requested-by-user
                                         budget-period-in-requesting-phase)
                                    category-inspectable-by-user
                                    user-is-admin))},
     :article_number {:read true,
                      :write (and request-without-template
                                  (not budget-period-is-past)
                                  (or (and user-is-requester
                                           requested-by-user
                                           budget-period-in-requesting-phase)
                                      category-inspectable-by-user
                                      user-is-admin))},
     :supplier {:read true,
                :write (and request-without-template
                            (not budget-period-is-past)
                            (or (and user-is-requester
                                     requested-by-user
                                     budget-period-in-requesting-phase)
                                category-inspectable-by-user
                                user-is-admin))},
     :receiver {:read true,
                :write (and (not budget-period-is-past)
                            (or (and user-is-requester
                                     requested-by-user
                                     budget-period-in-requesting-phase)
                                category-inspectable-by-user
                                user-is-admin))},
     :building_id {:read true,
                   :write (and (not budget-period-is-past)
                               (or (and user-is-requester
                                        requested-by-user
                                        budget-period-in-requesting-phase)
                                   category-inspectable-by-user
                                   user-is-admin))},
     ;; ==================================================================
     ;; TODO: temporary excluded from permissions check. Question is:
     ;; how to best reuse `room` / `room_id` which are used for reading
     ;; and writing respectively.
     ;
     ; :room {:read true,
     ;        :write (and (not budget-period-is-past)
     ;                    (or (and user-is-requester
     ;                             requested-by-user
     ;                             budget-period-in-requesting-phase)
     ;                        category-inspectable-by-user
     ;                        user-is-admin))},
     ; :model {:read true,
     ;         :write (and (not budget-period-is-past)
     ;                     (or (and user-is-requester
     ;                              requested-by-user
     ;                              budget-period-in-requesting-phase)
     ;                         category-inspectable-by-user
     ;                         user-is-admin))},
     ;; ==================================================================
     :motivation {:read true,
                  :write (and (not budget-period-is-past)
                              (or (and user-is-requester
                                       requested-by-user
                                       budget-period-in-requesting-phase)
                                  category-inspectable-by-user
                                  user-is-admin))},
     :requested_quantity {:read true,
                          :write (and
                                   (not budget-period-is-past)
                                   (or (and user-is-requester
                                            requested-by-user
                                            budget-period-in-requesting-phase)
                                       category-inspectable-by-user
                                       user-is-admin))},
     :approved_quantity
       {:read
          (or (and user-is-requester requested-by-user budget-period-is-past)
              user-is-inspector
              user-is-admin),
        :write (and (not budget-period-is-past)
                    (or category-inspectable-by-user user-is-admin))},
     :order_quantity {:read (or user-is-inspector user-is-admin),
                      :write (and (not budget-period-is-past)
                                  (or category-inspectable-by-user
                                      user-is-admin))},
     :inspection_comment
       {:read
          (or (and user-is-requester requested-by-user budget-period-is-past)
              user-is-inspector
              user-is-admin),
        :write (and (not budget-period-is-past)
                    (or category-inspectable-by-user user-is-admin))},
     :attachments {:read true,
                   :write (and (not budget-period-is-past)
                               (or (and user-is-requester
                                        requested-by-user
                                        budget-period-in-requesting-phase)
                                   category-inspectable-by-user
                                   user-is-admin))},
     :accounting_type {:read user-is-inspector,
                       :write (and (not budget-period-is-past)
                                   (or category-inspectable-by-user
                                       user-is-admin))},
     :internal_order_number {:read user-is-inspector,
                             :write (and (not budget-period-is-past)
                                         (or category-inspectable-by-user
                                             user-is-admin))}}))
