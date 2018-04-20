(ns leihs.admin.paths
  (:refer-clojure :exclude [str keyword])
  (:require
    [leihs.admin.utils.core :refer [keyword str presence]]
    [bidi.verbose :refer [branch param leaf]]
    [bidi.bidi :refer [path-for match-route]]
    [leihs.admin.utils.url.query-params :refer [encode-query-params]]

    #?@(:clj
         [[uritemplate-clj.core :as uri-templ]
          [clojure.tools.logging :as logging]
          [logbug.catcher :as catcher]
          [logbug.debug :as debug]
          [logbug.thrown :as thrown]
          ])))

(def external-handlers
  #{:admin-audits-legacy
    :admin-buildings
    :admin-fields
    :admin-inventory-pools
    :admin-languages
    :admin-mail-templates
    :admin-rooms
    :admin-settings
    :admin-statistics
    :admin-suppliers
    :borrow
    :lending
    :procurement
    })

(def delegation-paths
  (branch "/delegations"
          (leaf "/" :delegations)
          (branch "/add"
                  (leaf "" :delegation-add)
                  (leaf "/choose-responsible-user" :delegation-add-choose-responsible-user))
          (branch "/"
                  (param :delegation-id)
                  (leaf "" :delegation)
                  (leaf "/delete" :delegation-delete)
                  (branch "/edit"
                          (leaf "" :delegation-edit)
                          (leaf "/choose-responsible-user" :delegation-edit-choose-responsible-user))
                  (branch "/users"
                          (leaf "/" :delegation-users)
                          (leaf "/add" :delegation-users-add)
                          (branch "/"
                                  (param :user-id)
                                  (leaf "" :delegation-user))))))

(def users-paths 
  (branch "/users"
          (branch "/"
                  (leaf "" :users)
                  (leaf "new" :user-new))
          (branch "/" 
                  (param :user-id)
                  (leaf "" :user)
                  (leaf "/delete" :user-delete)
                  (leaf "/edit" :user-edit)
                  (branch "/api-tokens/"
                          (leaf "" :api-tokens)
                          (leaf "new" :api-token-new)
                          (branch ""
                                  (param :api-token-id)
                                  (leaf "" :api-token)
                                  (leaf "/delete" :api-token-delete)
                                  (leaf "/edit" :api-token-edit)))
                  (branch "/transfer/"
                          (param :target-user-id)
                          (leaf "" :user-transfer-data)))))

(def paths
  (branch ""
          (leaf "/" :home)
          (branch "/auth"
                  (leaf "" :auth)
                  (leaf "/shib-sign-in" :auth-shib-sign-in)
                  (leaf "/password-sign-in" :auth-password-sign-in)
                  (leaf "/sign-out" :auth-sign-out))
          (leaf "/procurement" :procurement)
          (leaf "/manage" :lending)
          (leaf "/borrow" :borrow)
          (branch "/admin"
                  (leaf "/status" :status)
                  (leaf "/shutdown" :shutdown)
                  (leaf "/initial-admin" :initial-admin)
                  (branch "/debug"
                          (leaf "" :debug)
                          (branch "/requests"
                                  (leaf "/" :requests)
                                  (branch "/" (param :id)
                                          (leaf "" :request))))
                  (leaf "/" :admin)
                  delegation-paths
                  users-paths
                  (leaf "/audits" :admin-audits-legacy)
                  (leaf "/buildings" :admin-buildings)
                  (leaf "/fields_editor" :admin-fields)
                  (leaf "/inventory_pools" :admin-inventory-pools)
                  (leaf "/languages" :admin-languages)
                  (leaf "/mail_templates" :admin-mail-templates)
                  (leaf "/rooms" :admin-rooms)
                  (leaf "/settings" :admin-settings)
                  (leaf "/statistics" :adming-statistics)
                  (leaf "/suppliers" :admin-suppliers)
                  )))

;(path-for (paths) :user :user-id "{user-id}")
;(match-route (paths) "/users/512")
;(match-route (paths) "/?x=5#7")

(defn path
  ([kw]
   (path-for paths kw))
  ([kw route-params]
   (apply (partial path-for paths kw)
          (->> route-params (into []) flatten)))
  ([kw route-params query-params]
   (str (path kw route-params) "?"
        (encode-query-params query-params))))

