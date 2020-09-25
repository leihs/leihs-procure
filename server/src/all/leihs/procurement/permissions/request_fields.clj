(ns leihs.procurement.permissions.request-fields
  (:require [leihs.procurement.resources.rooms :as rooms]
            [leihs.procurement.resources.building :as building]
            [leihs.procurement.resources.category :as category]
            [leihs.procurement.permissions.user :as user-perms]
            [leihs.procurement.resources.model :as model]
            [leihs.procurement.resources.template :as template]
            [leihs.procurement.resources.user :as user]
            [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log]
            [leihs.procurement.resources.budget-period :as budget-period]))

(defn get-for-user-and-request
  "Read permissions always apply only to an existing request.
  Write permissions apply either to a new or an existing request."
  [tx auth-entity proc-request]
  (let [new-request (nil? (:id proc-request))
        existing-request (not new-request)
        budget-period (budget-period/get-budget-period-by-id tx
                                                             (-> proc-request
                                                                 :budget_period
                                                                 :id))
        template (some->> proc-request
                          :template
                          :id
                          (template/get-template-by-id tx))
        category-id (or (-> proc-request
                            :category
                            :id)
                        (:category_id template))
        category (category/get-category-by-id tx category-id)
        user-id (-> proc-request
                    :user
                    :id)
        own-request (= (-> auth-entity
                           :user_id
                           str)
                       (str user-id))
        user (user/get-user-by-id tx user-id)
        requester (user-perms/requester? tx auth-entity)
        inspector (user-perms/inspector? tx auth-entity)
        admin (user-perms/admin? tx auth-entity)
        past-phase (budget-period/past? tx budget-period)
        requesting-phase (budget-period/in-requesting-phase? tx budget-period)
        inspection-phase (budget-period/in-inspection-phase? tx budget-period)
        category-inspector (user-perms/inspector? tx auth-entity category-id)
        category-viewer (user-perms/viewer? tx auth-entity category-id)]
    {:accounting_type
       {:read (or (and requester own-request (or inspection-phase past-phase))
                  category-viewer
                  inspector
                  admin),
        :write (and (not past-phase)
                    (or (and new-request
                             requester
                             (or (and requesting-phase inspector)
                                 (and inspection-phase category-inspector)
                                 admin))
                        (and existing-request (or admin category-inspector)))),
        :default "aquisition",
        :required true},
     :approved_quantity
       {:read (or (and requester own-request past-phase)
                  inspector
                  category-viewer
                  admin),
        :write (and (not past-phase)
                    (or
                      (and new-request requester (or admin category-inspector))
                      (and existing-request (or admin category-inspector)))),
        :required false},
     :article_name
       {:read (or (and requester own-request) category-viewer inspector admin),
        :write (and
                 (not template)
                 (not past-phase)
                 (or (and new-request
                          requester
                          (or (and requesting-phase (or own-request inspector))
                              (and inspection-phase category-inspector)
                              admin))
                     (and existing-request
                          (or admin
                              category-inspector
                              (and requesting-phase requester own-request))))),
        :default (:article_name template),
        :required true},
     :article_number
       {:read (or (and requester own-request) category-viewer inspector admin),
        :write (and
                 (not template)
                 (not past-phase)
                 (or (and new-request
                          requester
                          (or (and requesting-phase (or own-request inspector))
                              (and inspection-phase category-inspector)
                              admin))
                     (and existing-request
                          (or admin
                              category-inspector
                              (and requesting-phase requester own-request))))),
        :default (:article_number template),
        :required false},
     :attachments
       {:read (or (and requester own-request) category-viewer inspector admin),
        :write (and
                 (not past-phase)
                 (or (and new-request
                          requester
                          (or (and requesting-phase (or own-request inspector))
                              (and inspection-phase category-inspector)
                              admin))
                     (and existing-request
                          (or admin
                              category-inspector
                              (and requesting-phase requester own-request))))),
        :required false},
     :budget_period
       {:read true,
        :write (and
                 (not past-phase)
                 (or (and new-request
                          requester
                          (or (and requesting-phase (or own-request inspector))
                              (and inspection-phase category-inspector)
                              admin))
                     (and existing-request
                          (or admin
                              category-inspector
                              (and requesting-phase requester own-request))))),
        :default budget-period,
        :required true},
     :category
       {:read true,
        :write (and
                 (not past-phase)
                 (or (and new-request
                          requester
                          (or (and requesting-phase (or own-request inspector))
                              (and inspection-phase category-inspector)
                              admin))
                     (and existing-request
                          (or admin
                              category-inspector
                              (and requesting-phase requester own-request))))),
        :default category,
        :required true},
     :cost_center {:read (or (and requester own-request (or inspection-phase past-phase))
                             category-viewer
                             inspector
                             admin),
                   :write false,
                   :required false},
     :DELETE (and existing-request
                  (not past-phase)
                  (or admin
                      (and requesting-phase requester own-request)
                      category-inspector)),
     :general_ledger_account {:read  (or (and requester own-request (or inspection-phase past-phase))
                                         category-viewer
                                         inspector
                                         admin),
                              :write false,
                              :required false},
     :inspection_comment
       {:read (or (and requester own-request past-phase)
                  category-viewer
                  inspector
                  admin),
        :write (and (not past-phase)
                    (or (and new-request
                             requester
                             (or (and requesting-phase inspector)
                                 (and inspection-phase category-inspector)
                                 admin))
                        (and existing-request (or admin category-inspector)))),
        :required false},
     :inspector_priority
       {:read (or category-viewer inspector admin),
        :write (and (not past-phase)
                    (or (and new-request
                             requester
                             (or (and requesting-phase inspector)
                                 (and inspection-phase category-inspector)
                                 admin))
                        (and existing-request (or admin category-inspector)))),
        ; keep it upper-case!
        :default "MEDIUM",
        :required true},
     :internal_order_number
       {:read (or (and requester own-request (or inspection-phase past-phase))
                  category-viewer
                  inspector
                  admin),
        :write (and (not past-phase)
                    (or (and new-request
                             requester
                             (or (and requesting-phase inspector)
                                 (and inspection-phase category-inspector)
                                 admin))
                        (and existing-request (or admin category-inspector)))),
        :required false},
     :model
       {:read (or (and requester own-request) category-viewer inspector admin),
        :write (and
                 (not template)
                 (not past-phase)
                 (or (and new-request
                          requester
                          (or (and requesting-phase (or own-request inspector))
                              (and inspection-phase category-inspector)
                              admin))
                     (and existing-request
                          (or admin
                              category-inspector
                              (and requesting-phase requester own-request))))),
        :default (:model_id template),
        :required false},
     :motivation
       {:read (or (and requester own-request) category-viewer inspector admin),
        :write (and
                 (not past-phase)
                 (or (and new-request
                          requester
                          (or (and requesting-phase (or own-request inspector))
                              (and inspection-phase category-inspector)
                              admin))
                     (and existing-request
                          (or admin
                              (and requesting-phase requester own-request)
                              (and inspection-phase
                                   own-request
                                   category-inspector))))),
        :required true},
     :order_quantity
       {:read (or inspector category-viewer admin),
        :write (and (not past-phase)
                    (or
                      (and new-request requester (or admin category-inspector))
                      (and existing-request (or admin category-inspector)))),
        :required false},
     ; FIXME: remove from here?
     ; :organization {:read true,
     ;                :write (and new-request
     ;                            (or requester inspector admin)),
     ;                :required true},
     :price_cents
       {:read (or (and requester own-request) category-viewer inspector admin),
        :write (and
                 (not template)
                 (not past-phase)
                 (or (and new-request
                          requester
                          (or (and requesting-phase (or own-request inspector))
                              (and inspection-phase category-inspector)
                              admin))
                     (and existing-request
                          (or admin
                              category-inspector
                              (and requesting-phase requester own-request))))),
        :default (or (:price_cents template) 0),
        :required true},
     :price_currency {:read true,
                      :write false,
                      :default (or (:price_currency template) "CHF"),
                      :required true},
     :priority
       {:read (or (and requester own-request) category-viewer inspector admin),
        :write (and
                 (not past-phase)
                 (or (and new-request
                          requester
                          (or (and requesting-phase (or own-request inspector))
                              (and inspection-phase category-inspector)
                              admin))
                     (and existing-request
                          (or admin
                              (and requesting-phase requester own-request)
                              (and inspection-phase
                                   own-request
                                   category-inspector))))),
        :default "NORMAL",
        :required true},
     :procurement_account {:read (or (and requester own-request (or inspection-phase past-phase))
                                     category-viewer
                                     inspector
                                     admin),
                           :write false},
     :receiver
       {:read (or (and requester own-request) category-viewer inspector admin),
        :write (and
                 (not past-phase)
                 (or (and new-request
                          requester
                          (or (and requesting-phase (or own-request inspector))
                              (and inspection-phase category-inspector)
                              admin))
                     (and existing-request
                          (or admin
                              category-inspector
                              (and requesting-phase requester own-request))))),
        :required false},
     :replacement
       {:read (or (and requester own-request) category-viewer inspector admin),
        :write (and
                 (not past-phase)
                 (or (and new-request
                          requester
                          (or (and requesting-phase (or own-request inspector))
                              (and inspection-phase category-inspector)
                              admin))
                     (and existing-request
                          (or admin
                              category-inspector
                              (and requesting-phase requester own-request))))),
        :default nil,
        :required true},
     :requested_quantity
       {:read (or (and requester own-request) category-viewer inspector admin),
        :write
          (and
            (not past-phase)
            (or (or (and new-request
                         requester
                         (or (and requesting-phase (or own-request inspector))
                             (and inspection-phase category-inspector)
                             admin))
                    (and existing-request
                         (or admin
                             (and requesting-phase requester own-request)
                             (and inspection-phase category-inspector)))))),
        :required true},
     :room
       {:read (or (and requester own-request) category-viewer inspector admin),
        :write
          (and
            (not past-phase)
            (or (or (and new-request
                         requester
                         (or (and requesting-phase (or own-request inspector))
                             (and inspection-phase category-inspector)
                             admin))
                    (and existing-request
                         (or admin
                             category-inspector
                             (and requesting-phase requester own-request)))))),
        :default (rooms/general-from-general tx),
        :required true},
     :supplier
       {:read (or (and requester own-request) category-viewer inspector admin),
        :write (and
                 (not template)
                 (not past-phase)
                 (or (and new-request
                          requester
                          (or (and requesting-phase (or own-request inspector))
                              (and inspection-phase category-inspector)
                              admin))
                     (and existing-request
                          (or admin
                              category-inspector
                              (and requesting-phase requester own-request))))),
        :default (:supplier_id template),
        :required false},
     :supplier_name
       {:read (or (and requester own-request) category-viewer inspector admin),
        :write (and
                 (not template)
                 (not past-phase)
                 (or (and new-request
                          requester
                          (or (and requesting-phase (or own-request inspector))
                              (and inspection-phase category-inspector)
                              admin))
                     (and existing-request
                          (or admin
                              category-inspector
                              (and requesting-phase requester own-request))))),
        :default (:supplier_name template),
        :required false},
     :template {:read true,
                :write (not existing-request),
                :default template,
                :required true},
     :user {:read
              (or (and requester own-request) category-viewer inspector admin),
            :write
              (and (not past-phase)
                   (or (and new-request requester (or admin category-inspector))
                       (and existing-request (or admin category-inspector)))),
            :required true,
            :default user}}))
