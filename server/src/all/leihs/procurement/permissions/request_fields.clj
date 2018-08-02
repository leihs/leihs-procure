(ns leihs.procurement.permissions.request-fields
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log]
            [leihs.procurement.resources.budget-period :as budget-period]
            [leihs.procurement.permissions.user :as user-perms]
            [leihs.procurement.utils.ds :as ds]))

(defn get-for-user-and-request
  [tx auth-entity proc-request]
  "Read permissions always apply only to an existing request.
  Write permissions apply either to a new or an existing request."
  (let [request-exists (not (nil? (:id proc-request)))
        budget-period (budget-period/get-budget-period-by-id tx
                                                             (:budget_period
                                                               proc-request))
        request-without-template (not (:template proc-request))
        requested-by-user (= (:user_id auth-entity) (:user proc-request))
        user-is-requester (user-perms/requester? tx auth-entity)
        user-is-inspector (user-perms/inspector? tx auth-entity)
        user-is-admin (user-perms/admin? tx auth-entity)
        budget-period-is-past (budget-period/past? tx budget-period)
        budget-period-in-requesting-phase
          (budget-period/in-requesting-phase? tx budget-period)
        category-inspectable-by-user
          (user-perms/inspector? tx auth-entity (:category proc-request))
        category-viewable-by-user
          (user-perms/viewer? tx auth-entity (:category proc-request))]
    {:accounting_type
       {:read (or category-viewable-by-user user-is-inspector user-is-admin),
        :write (and (not budget-period-is-past)
                    (or category-inspectable-by-user user-is-admin)),
        :default "aquisition"},
     :approved_quantity
       {:read
          (or (and user-is-requester requested-by-user budget-period-is-past)
              user-is-inspector
              category-viewable-by-user
              user-is-admin),
        :write (and (not budget-period-is-past)
                    (or category-inspectable-by-user user-is-admin))},
     :article_name
       {:read (or (and user-is-requester requested-by-user)
                  category-viewable-by-user
                  user-is-inspector
                  user-is-admin),
        :write (and request-without-template
                    (not budget-period-is-past)
                    (or (and user-is-requester
                             (or (and request-exists requested-by-user) true)
                             budget-period-in-requesting-phase)
                        category-inspectable-by-user
                        user-is-admin))},
     :article_number
       {:read (or (and user-is-requester requested-by-user)
                  category-viewable-by-user
                  user-is-inspector
                  user-is-admin),
        :write (and request-without-template
                    (not budget-period-is-past)
                    (or (and user-is-requester
                             (or (and request-exists requested-by-user) true)
                             budget-period-in-requesting-phase)
                        category-inspectable-by-user
                        user-is-admin))},
     :attachments
       {:read (or (and user-is-requester requested-by-user)
                  category-viewable-by-user
                  user-is-inspector
                  user-is-admin),
        :write (and (not budget-period-is-past)
                    (or (and user-is-requester
                             (or (and request-exists requested-by-user) true)
                             budget-period-in-requesting-phase)
                        category-inspectable-by-user
                        user-is-admin))},
     :budget_period
       {:read true,
        :write (or ; existing request
                   (and request-exists
                        (not budget-period-is-past)
                        (or (and user-is-requester
                                 requested-by-user
                                 budget-period-in-requesting-phase)
                            category-inspectable-by-user
                            user-is-admin))
                   ; new request
                   (or user-is-requester user-is-inspector user-is-admin))},
     :category {:read true,
                :write
                  (or ; existing request
                      (and request-exists
                           (not budget-period-is-past)
                           (or (and user-is-requester
                                    requested-by-user
                                    budget-period-in-requesting-phase)
                               category-inspectable-by-user
                               user-is-admin))
                      ; new request
                      (or user-is-requester user-is-inspector user-is-admin))},
     :cost_center
       {:read (or category-viewable-by-user user-is-inspector user-is-admin),
        :write false},
     :general_ledger_account
       {:read (or category-viewable-by-user user-is-inspector user-is-admin),
        :write false},
     :inspection_comment
       {:read
          (or (and user-is-requester requested-by-user budget-period-is-past)
              category-viewable-by-user
              user-is-inspector
              user-is-admin),
        :write (and (not budget-period-is-past)
                    (or category-inspectable-by-user user-is-admin))},
     :inspector_priority {:read (or category-viewable-by-user
                                    user-is-inspector ; TODO: or
                                    ; category-inspectable-by-user
                                    ; ?
                                    user-is-admin),
                          :write (and (not budget-period-is-past)
                                      (or category-inspectable-by-user
                                          user-is-admin)),
                          :default "MEDIUM", ; keep it upper-case!
                          },
     :internal_order_number
       {:read (or category-viewable-by-user user-is-inspector user-is-admin),
        :write (and (not budget-period-is-past)
                    (or category-inspectable-by-user user-is-admin))},
     :model {:read (or (and user-is-requester requested-by-user)
                       category-viewable-by-user
                       user-is-inspector
                       user-is-admin),
             :write (and
                      (not budget-period-is-past)
                      (or (and user-is-requester
                               (or (and request-exists requested-by-user) true)
                               budget-period-in-requesting-phase)
                          category-inspectable-by-user
                          user-is-admin))},
     :motivation {:read (or (and user-is-requester requested-by-user)
                            category-viewable-by-user
                            user-is-inspector
                            user-is-admin),
                  :write (and user-is-requester
                              (or (and request-exists requested-by-user) true)
                              budget-period-in-requesting-phase)},
     :order_quantity
       {:read
          (or (and user-is-requester requested-by-user budget-period-is-past)
              user-is-inspector
              category-viewable-by-user
              user-is-admin),
        :write (and (not budget-period-is-past)
                    (or category-inspectable-by-user user-is-admin))},
     :organization
       {:read true,
        :write (and (not request-exists) ; can be set only for new
                    ; requests
                    (or user-is-requester user-is-inspector user-is-admin))},
     :price_cents
       {:read (or (and user-is-requester requested-by-user)
                  category-viewable-by-user
                  user-is-inspector
                  user-is-admin),
        :write (and (not budget-period-is-past)
                    (or (and user-is-requester
                             (or (and request-exists requested-by-user) true)
                             budget-period-in-requesting-phase)
                        category-inspectable-by-user
                        user-is-admin)),
        :default 0},
     :price_currency {:read true, :write false, :default "CHF"},
     :priority {:read (or (and user-is-requester requested-by-user)
                          category-viewable-by-user
                          user-is-inspector
                          user-is-admin),
                :write (and user-is-requester
                            (or (and request-exists requested-by-user) true)
                            budget-period-in-requesting-phase),
                :default "NORMAL"},
     :procurement_account
       {:read (or category-viewable-by-user user-is-inspector user-is-admin),
        :write false},
     :receiver {:read (or (and user-is-requester requested-by-user)
                          category-viewable-by-user
                          user-is-inspector
                          user-is-admin),
                :write (and (not budget-period-is-past)
                            (or (and user-is-requester
                                     (or (and request-exists requested-by-user)
                                         true)
                                     budget-period-in-requesting-phase)
                                category-inspectable-by-user
                                user-is-admin))},
     :replacement
       {:read (or (and user-is-requester requested-by-user)
                  category-viewable-by-user
                  user-is-inspector
                  user-is-admin),
        :write (and request-without-template
                    (not budget-period-is-past)
                    (or (and user-is-requester
                             (or (and request-exists requested-by-user) true)
                             budget-period-in-requesting-phase)
                        category-inspectable-by-user
                        user-is-admin)),
        :default true},
     :requested_quantity
       {:read (or (and user-is-requester requested-by-user)
                  category-viewable-by-user
                  user-is-inspector
                  user-is-admin),
        :write (and (not budget-period-is-past)
                    (or (and user-is-requester
                             (or (and request-exists requested-by-user) true)
                             budget-period-in-requesting-phase)
                        category-inspectable-by-user ; TODO: why?
                        user-is-admin))},
     :room {:read (or (and user-is-requester requested-by-user)
                      category-viewable-by-user
                      user-is-inspector
                      user-is-admin),
            :write (and
                     (not budget-period-is-past)
                     (or (and user-is-requester
                              (or (and request-exists requested-by-user) true)
                              budget-period-in-requesting-phase)
                         category-inspectable-by-user
                         user-is-admin))},
     :state {:read (or (and user-is-requester requested-by-user)
                       category-viewable-by-user
                       user-is-inspector
                       user-is-admin),
             :write false},
     :supplier {:read (or (and user-is-requester requested-by-user)
                          category-viewable-by-user
                          user-is-inspector
                          user-is-admin),
                :write (and request-without-template
                            (not budget-period-is-past)
                            (or (and user-is-requester
                                     (or (and request-exists requested-by-user)
                                         true)
                                     budget-period-in-requesting-phase)
                                category-inspectable-by-user
                                user-is-admin))},
     :user {:read true,
            :write
              (and (not request-exists) ; can be set only for new requests
                   (or user-is-requester user-is-inspector user-is-admin))}}))
