(ns leihs.procurement.permissions.request-fields
  (:require [leihs.procurement.resources.rooms :as rooms]
            [leihs.procurement.resources.category :as category]
            [leihs.procurement.permissions.user :as user-perms]
            [leihs.procurement.resources.model :as model]
            [leihs.procurement.resources.template :as template]
            [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log]
            [leihs.procurement.resources.budget-period :as budget-period]))

(defn get-for-user-and-request
  [tx auth-entity proc-request]
  "Read permissions always apply only to an existing request.
  Write permissions apply either to a new or an existing request."
  (let [request-exists (not (nil? (:id proc-request)))
        budget-period (budget-period/get-budget-period-by-id tx
                                                             (:budget_period
                                                               proc-request))
        template (some->> proc-request
                          :template
                          (template/get-template-by-id tx))
        category-id (or (:category proc-request) (:category_id template))
        request-without-template (not (:template proc-request))
        requested-by-auth-user (= (:user_id auth-entity) (:user proc-request))
        auth-user-is-requester (user-perms/requester? tx auth-entity)
        auth-user-is-inspector (user-perms/inspector? tx auth-entity)
        auth-user-is-admin (user-perms/admin? tx auth-entity)
        budget-period-is-past (budget-period/past? tx budget-period)
        budget-period-in-requesting-phase
          (budget-period/in-requesting-phase? tx budget-period)
        category-inspectable-by-auth-user
          (user-perms/inspector? tx auth-entity category-id)
        category-viewable-by-auth-user
          (user-perms/viewer? tx auth-entity category-id)]
    {:accounting_type {:read (or category-viewable-by-auth-user
                                 auth-user-is-inspector
                                 auth-user-is-admin),
                       :write (and (not budget-period-is-past)
                                   (or category-inspectable-by-auth-user
                                       auth-user-is-admin)),
                       :default "aquisition",
                       :required true},
     :approved_quantity {:read (or (and auth-user-is-requester
                                        requested-by-auth-user
                                        budget-period-is-past)
                                   auth-user-is-inspector
                                   category-viewable-by-auth-user
                                   auth-user-is-admin),
                         :write (and (not budget-period-is-past)
                                     (or category-inspectable-by-auth-user
                                         auth-user-is-admin)),
                         :required false},
     :article_name {:read (or (and auth-user-is-requester
                                   requested-by-auth-user)
                              category-viewable-by-auth-user
                              auth-user-is-inspector
                              auth-user-is-admin),
                    :write (and request-without-template
                                (not budget-period-is-past)
                                (or (and auth-user-is-requester
                                         budget-period-in-requesting-phase)
                                    category-inspectable-by-auth-user
                                    auth-user-is-admin)),
                    :default (:article_name template),
                    :required true},
     :article_number {:read (or (and auth-user-is-requester
                                     requested-by-auth-user)
                                category-viewable-by-auth-user
                                auth-user-is-inspector
                                auth-user-is-admin),
                      :write (and request-without-template
                                  (not budget-period-is-past)
                                  (or (and auth-user-is-requester
                                           budget-period-in-requesting-phase)
                                      category-inspectable-by-auth-user
                                      auth-user-is-admin)),
                      :default (:article_number template),
                      :required false},
     :attachments {:read (or (and auth-user-is-requester requested-by-auth-user)
                             category-viewable-by-auth-user
                             auth-user-is-inspector
                             auth-user-is-admin),
                   :write (and (not budget-period-is-past)
                               (or (and auth-user-is-requester
                                        budget-period-in-requesting-phase)
                                   category-inspectable-by-auth-user
                                   auth-user-is-admin)),
                   :required false},
     :budget_period {:read true,
                     :write (or ; existing request
                                (and request-exists
                                     (not budget-period-is-past)
                                     (or (and auth-user-is-requester
                                              requested-by-auth-user
                                              budget-period-in-requesting-phase)
                                         category-inspectable-by-auth-user
                                         auth-user-is-admin))
                                ; new request
                                (or auth-user-is-requester
                                    auth-user-is-inspector
                                    auth-user-is-admin)),
                     :default (:id budget-period),
                     :required true},
     :category {:read true,
                :write (or ; existing request
                           (and request-exists
                                (not budget-period-is-past)
                                (or (and auth-user-is-requester
                                         requested-by-auth-user
                                         budget-period-in-requesting-phase)
                                    category-inspectable-by-auth-user
                                    auth-user-is-admin))
                           ; new request
                           (or auth-user-is-requester
                               auth-user-is-inspector
                               auth-user-is-admin)),
                :default category-id,
                :required true},
     :cost_center {:read (or category-viewable-by-auth-user
                             auth-user-is-inspector
                             auth-user-is-admin),
                   :write false,
                   :required false},
     :general_ledger_account {:read (or category-viewable-by-auth-user
                                        auth-user-is-inspector
                                        auth-user-is-admin),
                              :write false,
                              :required false},
     :inspection_comment {:read (or (and auth-user-is-requester
                                         requested-by-auth-user
                                         budget-period-is-past)
                                    category-viewable-by-auth-user
                                    auth-user-is-inspector
                                    auth-user-is-admin),
                          :write (and (not budget-period-is-past)
                                      (or category-inspectable-by-auth-user
                                          auth-user-is-admin)),
                          :required false},
     :inspector_priority {:read (or category-viewable-by-auth-user
                                    auth-user-is-inspector ; TODO: or
                                    ; category-inspectable-by-auth-user
                                    ; ?
                                    auth-user-is-admin),
                          :write (and (not budget-period-is-past)
                                      (or category-inspectable-by-auth-user
                                          auth-user-is-admin)),
                          :default "MEDIUM", ; keep it upper-case!
                          :required true},
     :internal_order_number {:read (or category-viewable-by-auth-user
                                       auth-user-is-inspector
                                       auth-user-is-admin),
                             :write (and (not budget-period-is-past)
                                         (or category-inspectable-by-auth-user
                                             auth-user-is-admin)),
                             :required false},
     :model {:read (or (and auth-user-is-requester requested-by-auth-user)
                       category-viewable-by-auth-user
                       auth-user-is-inspector
                       auth-user-is-admin),
             :write (and request-without-template
                         (not budget-period-is-past)
                         (or (and auth-user-is-requester
                                  budget-period-in-requesting-phase)
                             category-inspectable-by-auth-user
                             auth-user-is-admin)),
             :default (:model_id template),
             :required false},
     :motivation {:read (or (and auth-user-is-requester requested-by-auth-user)
                            category-viewable-by-auth-user
                            auth-user-is-inspector
                            auth-user-is-admin),
                  :write (and (not budget-period-is-past)
                              (or auth-user-is-admin
                                  category-inspectable-by-auth-user
                                  (and auth-user-is-requester
                                       budget-period-in-requesting-phase))),
                  :required true},
     :order_quantity {:read (or (and auth-user-is-requester
                                     requested-by-auth-user
                                     budget-period-is-past)
                                auth-user-is-inspector
                                category-viewable-by-auth-user
                                auth-user-is-admin),
                      :write (and (not budget-period-is-past)
                                  (or category-inspectable-by-auth-user
                                      auth-user-is-admin)),
                      :required false},
     :organization {:read true,
                    :write (and (not request-exists) ; can be set only for new
                                ; requests
                                (or auth-user-is-requester
                                    auth-user-is-inspector
                                    auth-user-is-admin)),
                    :required true},
     :price_cents {:read (or (and auth-user-is-requester requested-by-auth-user)
                             category-viewable-by-auth-user
                             auth-user-is-inspector
                             auth-user-is-admin),
                   :write (and request-without-template
                               (not budget-period-is-past)
                               (or (and auth-user-is-requester
                                        budget-period-in-requesting-phase)
                                   category-inspectable-by-auth-user
                                   auth-user-is-admin)),
                   :default (or (:price_cents template) 0),
                   :required true},
     :price_currency {:read true,
                      :write request-without-template,
                      :default (or (:price_currency template) "CHF"),
                      :required true},
     :priority {:read (or (and auth-user-is-requester requested-by-auth-user)
                          category-viewable-by-auth-user
                          auth-user-is-inspector
                          auth-user-is-admin),
                :write (and auth-user-is-requester
                            budget-period-in-requesting-phase),
                :default "NORMAL",
                :required true},
     :procurement_account {:read (or category-viewable-by-auth-user
                                     auth-user-is-inspector
                                     auth-user-is-admin),
                           :write false},
     :receiver {:read (or (and auth-user-is-requester requested-by-auth-user)
                          category-viewable-by-auth-user
                          auth-user-is-inspector
                          auth-user-is-admin),
                :write (and (not budget-period-is-past)
                            (or (and auth-user-is-requester
                                     budget-period-in-requesting-phase)
                                category-inspectable-by-auth-user
                                auth-user-is-admin)),
                :required false},
     :replacement {:read (or (and auth-user-is-requester requested-by-auth-user)
                             category-viewable-by-auth-user
                             auth-user-is-inspector
                             auth-user-is-admin),
                   :write (and (not budget-period-is-past)
                               (or (and auth-user-is-requester
                                        budget-period-in-requesting-phase)
                                   category-inspectable-by-auth-user
                                   auth-user-is-admin)),
                   :default nil,
                   :required true},
     :requested_quantity
       {:read (or (and auth-user-is-requester requested-by-auth-user)
                  category-viewable-by-auth-user
                  auth-user-is-inspector
                  auth-user-is-admin),
        :write (and (not budget-period-is-past)
                    (or (and auth-user-is-requester
                             budget-period-in-requesting-phase)
                        category-inspectable-by-auth-user ; TODO: why?
                        auth-user-is-admin)),
        :required true},
     :room {:read (or (and auth-user-is-requester requested-by-auth-user)
                      category-viewable-by-auth-user
                      auth-user-is-inspector
                      auth-user-is-admin),
            :write (and (not budget-period-is-past)
                        (or (and auth-user-is-requester
                                 budget-period-in-requesting-phase)
                            category-inspectable-by-auth-user
                            auth-user-is-admin)),
            :default (:id (rooms/general-from-general tx)),
            :required true},
     :supplier {:read (or (and auth-user-is-requester requested-by-auth-user)
                          category-viewable-by-auth-user
                          auth-user-is-inspector
                          auth-user-is-admin),
                :write (and request-without-template
                            (not budget-period-is-past)
                            (or (and auth-user-is-requester
                                     budget-period-in-requesting-phase)
                                category-inspectable-by-auth-user
                                auth-user-is-admin)),
                :default (:supplier_id template),
                :required false},
     :supplier_name {:read (or (and auth-user-is-requester
                                    requested-by-auth-user)
                               category-viewable-by-auth-user
                               auth-user-is-inspector
                               auth-user-is-admin),
                     :write (and request-without-template
                                 (not budget-period-is-past)
                                 (or (and auth-user-is-requester
                                          budget-period-in-requesting-phase)
                                     category-inspectable-by-auth-user
                                     auth-user-is-admin)),
                     :default (:supplier_name template),
                     :required false},
     :template {:read true,
                :write (not request-exists),
                :default (:id template),
                :required true},
     :user {:read (or category-viewable-by-auth-user
                      auth-user-is-inspector
                      auth-user-is-admin),
            :write (and (not budget-period-is-past)
                        (or category-inspectable-by-auth-user
                            auth-user-is-admin)),
            :required true,
            :default (:user_id auth-entity)}}))
