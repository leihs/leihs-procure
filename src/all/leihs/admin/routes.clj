(ns leihs.admin.routes
  (:refer-clojure :exclude [str keyword])
  (:require [clj-logging-config.log4j :as logging-config]
            [leihs.core.core :refer [keyword str presence]])
  (:require
   [leihs.core.anti-csrf.back :as anti-csrf]
   [leihs.core.auth.core :as auth]
   [leihs.core.constants :as constants]
   [leihs.core.ds :as ds]
   [leihs.core.http-cache-buster2 :as cache-buster :refer [wrap-resource]]
   [leihs.core.json :as json]
   [leihs.core.json-protocol]
   [leihs.core.ring-audits :as ring-audits]
   [leihs.core.ring-exception :as ring-exception]
   [leihs.core.routes :as core-routes :refer [all-granted]]
   [leihs.core.routing.back :as routing]
   [leihs.core.routing.dispatch-content-type :as dispatch-content-type]
   [leihs.core.settings :as settings]

   [leihs.admin.html :as html]
   [leihs.admin.env :as env]
   [leihs.admin.paths :refer [path paths]]


   [leihs.admin.resources.audits.changes.main :as audited-changes]
   [leihs.admin.resources.audits.changes.change.main :as audited-change]
   [leihs.admin.resources.audits.requests.main :as audited-requests]
   [leihs.admin.resources.audits.requests.request.main :as audited-request]

   [leihs.admin.resources.inventory-pools.authorization :as pool-auth]

   [leihs.admin.resources.groups.group.main :as group]
   [leihs.admin.resources.groups.group.users.main :as group-users]
   [leihs.admin.resources.groups.main :as groups]
   [leihs.admin.resources.inventory-pools.main :as inventory-pools]
   [leihs.admin.resources.inventory-pools.inventory-pool.delegations.main :as pool-delegations]
   [leihs.admin.resources.inventory-pools.inventory-pool.delegations.delegation.main :as pool-delegation]
   [leihs.admin.resources.inventory-pools.inventory-pool.delegations.delegation.groups.main :as pool-delegation-groups]
   [leihs.admin.resources.inventory-pools.inventory-pool.delegations.delegation.suspension.main :as inventory-pool-delegation-suspension]
   [leihs.admin.resources.inventory-pools.inventory-pool.delegations.delegation.users.main :as pool-delegation-users]
   [leihs.admin.resources.inventory-pools.inventory-pool.entitlement-groups.main :as entitlement-groups]
   [leihs.admin.resources.inventory-pools.inventory-pool.entitlement-groups.entitlement-group.main :as entitlement-group]
   [leihs.admin.resources.inventory-pools.inventory-pool.entitlement-groups.entitlement-group.users.main :as entitlement-group-users]
   [leihs.admin.resources.inventory-pools.inventory-pool.entitlement_groups.entitlement_group.groups.main :as entitlement-group-groups]
   [leihs.admin.resources.inventory-pools.inventory-pool.groups.main :as inventory-pool-groups]
   [leihs.admin.resources.inventory-pools.inventory-pool.groups.group.roles.main :as inventory-pool-group-roles]
   [leihs.admin.resources.inventory-pools.inventory-pool.users.main :as inventory-pool-users]
   [leihs.admin.resources.inventory-pools.inventory-pool.users.user.main :as inventory-pool-user]
   [leihs.admin.resources.inventory-pools.inventory-pool.users.user.direct-roles.main :as inventory-pool-user-direct-roles]
   [leihs.admin.resources.inventory-pools.inventory-pool.users.user.groups-roles.main :as inventory-pool-user-groups-roles]
   [leihs.admin.resources.inventory-pools.inventory-pool.users.user.roles.main :as inventory-pool-user-roles]
   [leihs.admin.resources.inventory-pools.inventory-pool.users.user.suspension.main :as inventory-pool-user-suspension]
   [leihs.admin.resources.status.main :as status]

   [leihs.admin.resources.system.authentication-systems.authentication-system.groups.main :as authentication-system-groups]
   [leihs.admin.resources.system.authentication-systems.authentication-system.main :as authentication-system]
   [leihs.admin.resources.system.authentication-systems.authentication-system.users.main :as authentication-system-users]
   [leihs.admin.resources.system.authentication-systems.main :as authentication-systems]

   [leihs.admin.resources.settings.languages.main :as languages-settings]
   [leihs.admin.resources.settings.misc.main :as misc-settings]
   [leihs.admin.resources.settings.smtp.main :as smtp-settings]
   [leihs.admin.resources.settings.syssec.main :as syssec-settings]

   [leihs.admin.resources.users.main :as users]
   [leihs.admin.resources.users.user.main :as user]
   [leihs.admin.resources.users.user.inventory-pools :as user-inventory-pools]

   [leihs.admin.resources.statistics.basic :as statistics-basic]

   [bidi.bidi :as bidi]
   [bidi.ring :refer [make-handler]]
   [compojure.core :as cpj]
   [ring.middleware.accept]
   [ring.middleware.content-type :refer [wrap-content-type]]
   [ring.middleware.cookies]
   [ring.middleware.json]
   [ring.middleware.params]
   [ring.util.response :refer [redirect]]

   [clojure.tools.logging :as logging]
   [logbug.catcher :as catcher]
   [logbug.debug :as debug :refer [I>]]
   [logbug.ring :refer [wrap-handler-with-logging]]
   [logbug.thrown :as thrown]

   ))

(declare redirect-to-root-handler)

(def skip-authorization-handler-keys
  (clojure.set/union
    core-routes/skip-authorization-handler-keys
    #{:home}))

(def no-spa-handler-keys
  (clojure.set/union
    core-routes/no-spa-handler-keys
    #{:redirect-to-root
      :not-found}))


(def resolve-table
  (merge core-routes/resolve-table
         {:audited-changes {:handler audited-changes/routes
                            :authorizers [auth/system-admin-scopes?]}

          :audited-changes-meta {:handler audited-changes/routes
                                 :authorizers [auth/system-admin-scopes?]}

          :audited-change {:handler audited-change/routes
                           :authorizers [auth/system-admin-scopes?]}

          :audited-requests {:handler audited-requests/routes
                             :authorizers [auth/system-admin-scopes?]}

          :audited-request {:handler audited-request/routes
                            :authorizers [auth/system-admin-scopes?]}

          :authentication-system {:handler authentication-system/routes
                                  :authorizers [auth/system-admin-scopes?]}
          :authentication-system-group {:handler authentication-system-groups/routes
                                        :authorizers [auth/admin-scopes?]}
          :authentication-system-groups {:handler authentication-system-groups/routes
                                         :authorizers [auth/admin-scopes?]}
          :authentication-system-user {:handler authentication-system-users/routes
                                       :authorizers [auth/admin-scopes?]}
          :authentication-system-users {:handler authentication-system-users/routes
                                        :authorizers [auth/admin-scopes?]}
          :authentication-systems {:handler authentication-systems/routes
                                   :authorizers [auth/system-admin-scopes?]}
          :group {:handler group/routes :authorizers [auth/admin-scopes? pool-auth/some-lending-manager?]}
          :group-inventory-pools-roles {:handler group/routes :authorizers [auth/admin-scopes? pool-auth/some-lending-manager?]}
          :group-user {:handler group-users/routes :authorizers [auth/admin-scopes? pool-auth/some-lending-manager?]}
          :group-users {:handler group-users/routes :authorizers [auth/admin-scopes? pool-auth/some-lending-manager?]}
          :groups {:handler groups/routes :authorizers [auth/admin-scopes? pool-auth/some-lending-manager?]}
          :inventory-pool {:handler inventory-pools/routes
                           :authorizers [auth/admin-scopes?
                                         pool-auth/pool-inventory-manager?
                                         pool-auth/pool-lending-manager-and-http-safe?]}
          :inventory-pool-delegation {:handler pool-delegation/routes
                                      :authorizers [auth/admin-scopes? pool-auth/pool-lending-manager?]}
          :inventory-pool-delegation-group {:handler pool-delegation-groups/routes
                                            :authorizers [auth/admin-scopes?
                                                          pool-auth/pool-lending-manager?]}
          :inventory-pool-delegation-groups {:handler pool-delegation-groups/routes
                                             :authorizers [auth/admin-scopes?
                                                           pool-auth/pool-lending-manager?]}
          :inventory-pool-delegation-suspension {:handler inventory-pool-delegation-suspension/routes
                                                 :authorizers [auth/admin-scopes?
                                                               pool-auth/pool-lending-manager?]}
          :inventory-pool-delegation-user {:handler pool-delegation-users/routes
                                           :authorizers [auth/admin-scopes?
                                                         pool-auth/pool-lending-manager?]}
          :inventory-pool-delegation-users {:handler pool-delegation-users/routes
                                            :authorizers [auth/admin-scopes?
                                                          pool-auth/pool-lending-manager?]}
          :inventory-pool-delegations {:handler pool-delegations/routes
                                       :authorizers [auth/admin-scopes?
                                                     pool-auth/pool-lending-manager?]}

          :inventory-pool-entitlement-group {:handler entitlement-group/routes
                                           :authorizers [auth/admin-scopes? pool-auth/pool-lending-manager?]}
          :inventory-pool-entitlement-group-direct-user {:handler entitlement-group-users/routes
                                                       :authorizers [auth/admin-scopes? pool-auth/pool-lending-manager?]}
          :inventory-pool-entitlement-group-group {:handler entitlement-group-groups/routes
                                                 :authorizers [auth/admin-scopes? pool-auth/pool-lending-manager?]}
          :inventory-pool-entitlement-group-groups {:handler entitlement-group-groups/routes
                                                  :authorizers [auth/admin-scopes?  pool-auth/pool-lending-manager?]}
          :inventory-pool-entitlement-group-users {:handler entitlement-group-users/routes
                                                 :authorizers [auth/admin-scopes? pool-auth/pool-lending-manager?]}
          :inventory-pool-entitlement-groups {:handler entitlement-groups/routes
                                            :authorizers [auth/admin-scopes?  pool-auth/pool-lending-manager?]}
          :inventory-pool-entitlement-groups-group {:handler entitlement-groups/routes
                                                  :authorizers [auth/admin-scopes? pool-auth/pool-lending-manager?]}


          :inventory-pool-group-roles {:handler inventory-pool-group-roles/routes :authorizers [auth/admin-scopes? pool-auth/pool-lending-manager?]}
          :inventory-pool-groups {:handler inventory-pool-groups/routes :authorizers [auth/admin-scopes? pool-auth/pool-lending-manager?]}
          :inventory-pool-user-direct-roles {:handler inventory-pool-user-direct-roles/routes :authorizers [auth/admin-scopes? pool-auth/pool-lending-manager?]}
          :inventory-pool-user-groups-roles {:handler inventory-pool-user-groups-roles/routes :authorizers [auth/admin-scopes? pool-auth/pool-lending-manager?]}
          :inventory-pool-user-roles {:handler inventory-pool-user-roles/routes :authorizers [auth/admin-scopes? pool-auth/pool-lending-manager?]}
          :inventory-pool-user {:handler inventory-pool-user/routes :authorizers [auth/admin-scopes? pool-auth/pool-lending-manager?]}
          :inventory-pool-user-suspension {:handler inventory-pool-user-suspension/routes :authorizers [auth/admin-scopes? pool-auth/pool-lending-manager?]}
          :inventory-pool-users {:handler inventory-pool-users/routes :authorizers [auth/admin-scopes? pool-auth/pool-lending-manager?]}
          :inventory-pools {:handler inventory-pools/routes
                            :authorizers [auth/admin-scopes?
                                          pool-auth/some-lending-manager-and-http-safe?]}
          :not-found {:handler html/not-found-handler :authorizers [all-granted]}
          :redirect-to-root {:handler redirect-to-root-handler :authorizers [all-granted]}
          :statistics-basic {:handler statistics-basic/routes :authorizers [auth/admin-scopes?]}
          :status {:handler status/routes :authorizers [all-granted]}

          :languages-settings {:handler languages-settings/routes
                          :authorizers [auth/admin-scopes?]}
          :misc-settings {:handler misc-settings/routes
                          :authorizers [auth/admin-scopes?]}
          :smtp-settings {:handler smtp-settings/routes
                          :authorizers [auth/system-admin-scopes?]}
          :syssec-settings {:handler syssec-settings/routes
                          :authorizers [auth/system-admin-scopes?]}


          :user {:handler user/routes :authorizers [auth/admin-scopes? pool-auth/some-lending-manager?]}
          :user-inventory-pools {:handler user-inventory-pools/routes :authorizers [auth/admin-scopes? pool-auth/some-lending-manager?]}
          :user-transfer-data {:handler user/routes :authorizers [auth/admin-scopes? pool-auth/some-lending-manager?]}
          :users {:handler users/routes :authorizers [auth/admin-scopes? pool-auth/some-lending-manager?]}
          :users-choose {:handler users/routes :authorizers [auth/admin-scopes? pool-auth/some-lending-manager?]}
          }))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn redirect-to-root-handler [request]
  (redirect (path :root)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn browser-request-matches-javascript? [request]
  "Returns true if the accepted type is javascript or
  if the :uri ends with .js. Note that browsers do not
  use the proper accept type for javascript script tags."
  (boolean (or (= (-> request :accept :mime) :javascript)
               (re-find #".+\.js$" (or (-> request :uri presence) "")))))

(defn wrap-dispatch-content-type
  ([handler]
   (fn [request]
     (wrap-dispatch-content-type handler request)))
  ([handler request]
   (cond
     ; accept json always goes to the backend handlers, i.e. the normal routing
     (= (-> request :accept :mime) :json) (or (handler request)
                                              (throw (ex-info "This resource does not provide a json response."
                                                              {:status 406})))
     ; accept HTML and GET (or HEAD) wants allmost always the frontend
     (and (= (-> request :accept :mime) :html)
          (#{:get :head} (:request-method request))
          (not (no-spa-handler-keys (:handler-key request)))
          (not (browser-request-matches-javascript? request))
          ) (html/html-handler request)
     ; other request might need to go the backend and return frontend nevertheless
     :else (let [response (handler request)]
             (if (and (nil? response)
                      ; TODO we might not need the following after we check (?nil response)
                      (not (no-spa-handler-keys (:handler-key request)))
                      (not (#{:post :put :patch :delete} (:request-method request)))
                      (= (-> request :accept :mime) :html)
                      (not (browser-request-matches-javascript? request)))
               (html/html-handler request)
               response)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn canonicalize-params-map [params & {:keys [parse-json?]
                                         :or {parse-json? true}}]
  (if-not (map? params)
    params
    (->> params
         (map (fn [[k v]]
                [(keyword k)
                 (if parse-json? (json/try-parse-json v) v)]))
         (into {}))))

(defn wrap-empty [handler]
  (fn [request]
    (or (handler request)
        {:status 404})))

(defn wrap-secret-byte-array
  "Adds the secret into the request as a byte-array (to prevent
  visibility in logs etc) under the :secret-byte-array key."
  [handler secret]
  (fn [request]
    (handler (assoc request :secret-ba (.getBytes secret)))))

(defn init [secret]
  (routing/init paths resolve-table)
  (I> wrap-handler-with-logging
      routing/dispatch-to-handler
      (auth/wrap resolve-table)
      wrap-dispatch-content-type
      ring-audits/wrap
      anti-csrf/wrap
      auth/wrap-authenticate
      ring.middleware.cookies/wrap-cookies
      wrap-empty
      (wrap-secret-byte-array secret)
      settings/wrap
      ds/wrap-tx
      status/wrap
      ring.middleware.json/wrap-json-response
      (ring.middleware.json/wrap-json-body {:keywords? true})
      dispatch-content-type/wrap-accept
      routing/wrap-add-vary-header
      routing/wrap-resolve-handler
      routing/wrap-canonicalize-params-maps
      ring.middleware.params/wrap-params
      wrap-content-type
      (wrap-resource
        "public" {:allow-symlinks? true
                  :cache-bust-paths ["/admin/css/site.css"
                                     "/admin/css/site.min.css"
                                     "/admin/js/main.js"
                                     "/admin/leihs-shared-bundle.js"]
                  :never-expire-paths [#".*fontawesome-[^\/]*\d+\.\d+\.\d+\/.*"
                                       #".+_[0-9a-f]{40}\..+"]
                  :enabled? true})
      ring-exception/wrap))

;#### debug ###################################################################
;(logging-config/set-logger! :level :debug)
;(logging-config/set-logger! :level :info)
;(debug/debug-ns *ns*)
