(ns leihs.admin.routes
  (:refer-clojure :exclude [str keyword])
  (:require [clj-logging-config.log4j :as logging-config]
            [leihs.core.core :refer [keyword str presence]])
  (:require
   [leihs.core.anti-csrf.back :as anti-csrf]
   [leihs.core.constants :as constants]
   [leihs.core.ds :as ds]
   [leihs.core.http-cache-buster :as cache-buster :refer [wrap-resource]]
   [leihs.core.json :as json]
   [leihs.core.json-protocol]
   [leihs.core.ring-exception :as ring-exception]
   [leihs.core.shutdown :as shutdown]
   [leihs.core.auth.core :as auth]
   [leihs.core.sign-out.back :as sign-out]

   [leihs.admin.auth.back :as admin-auth]
   [leihs.admin.back.html :as html]
   [leihs.admin.env :as env]
   [leihs.admin.paths :refer [path paths]]
   [leihs.admin.resources.authentication-system.back :as authentication-system]
   [leihs.admin.resources.authentication-system.groups.back :as authentication-system-groups]
   [leihs.admin.resources.authentication-system.users.back :as authentication-system-users]
   [leihs.admin.resources.authentication-systems.back :as authentication-systems]
   [leihs.admin.resources.delegation.back :as delegation]
   [leihs.admin.resources.delegation.users.back :as delegation-users]
   [leihs.admin.resources.delegations.back :as delegations]
   [leihs.admin.resources.group.back :as group]
   [leihs.admin.resources.group.users.back :as group-users]
   [leihs.admin.resources.groups.back :as groups]
   [leihs.admin.resources.settings.back :as settings]
   [leihs.admin.resources.status.back :as status]
   [leihs.admin.resources.system-admins.back :as system-admins]
   [leihs.admin.resources.system-admins.direct-users.back :as system-admin-direct-users]
   [leihs.admin.resources.system-admins.groups.back :as system-admin-groups]
   [leihs.admin.resources.user.back :as user]
   [leihs.admin.resources.users.back :as users]

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
   [logbug.thrown :as thrown]))

(declare redirect-to-root-handler)

(def skip-authorization-handler-keys
  #{:home
    :password-authentication
    :initial-admin})

(def do-not-dispatch-to-std-frontend-handler-keys
  #{:redirect-to-root
    :not-found})

(def handler-resolve-table
  {:password-authentication admin-auth/routes
   :auth-sign-out admin-auth/routes
   :authentication-system authentication-system/routes
   :authentication-system-groups authentication-system-groups/routes
   :authentication-system-group authentication-system-groups/routes
   :authentication-system-user authentication-system-users/routes
   :authentication-system-users authentication-system-users/routes
   :authentication-systems authentication-systems/routes
   :delegation delegation/routes
   :delegation-add-choose-responsible-user delegation/routes
   :delegation-edit-choose-responsible-user delegation/routes
   :delegation-user delegation-users/routes
   :delegation-users delegation-users/routes
   :delegations delegations/routes
   :group group/routes
   :group-user group-users/routes
   :group-users group-users/routes
   :groups groups/routes
   :not-found html/not-found-handler
   :redirect-to-root redirect-to-root-handler
   :sign-out sign-out/ring-handler
   :status status/routes
   :system-admins system-admins/routes
   :system-admins-direct-user system-admin-direct-users/routes
   :system-admin-direct-users system-admin-direct-users/routes
   :system-admins-group system-admin-groups/routes
   :system-admin-groups system-admin-groups/routes
   :user user/routes
   :user-inventory-pools-roles user/routes
   :user-transfer-data user/routes
   :users users/routes })



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn redirect-to-root-handler [request]
  (redirect (path :root)))

(defn handler-resolver [handler-key]
  (get handler-resolve-table handler-key nil))

(defn dispatch-to-handler [request]
  (if-let [handler (:handler request)]
    (handler request)
    (throw
      (ex-info
        "There is no handler for this resource and the accepted content type."
        {:status 404}))))


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
          (not (do-not-dispatch-to-std-frontend-handler-keys
                 (:handler-key request)))
          (not (browser-request-matches-javascript? request))
          ) (html/html-handler request)
     ; other request might need to go the backend and return frontend nevertheless
     :else (let [response (handler request)]
             (if (and (nil? response)
                      ; TODO we might not need the following after we check (?nil response)
                      (not (do-not-dispatch-to-std-frontend-handler-keys
                             (:handler-key request)))
                      (not (#{:post :put :patch :delete} (:request-method request)))
                      (= (-> request :accept :mime) :html)
                      (not (browser-request-matches-javascript? request)))
               (html/html-handler request)
               response)))))

(defn wrap-resolve-handler
  ([handler]
   (fn [request]
     (wrap-resolve-handler handler request)))
  ([handler request]
   (let [path (or (-> request :path-info presence)
                  (-> request :uri presence))
         {route-params :route-params
          handler-key :handler} (bidi/match-pair paths {:remainder path
                                                        :route paths})
         handler-fn (handler-resolver handler-key)]
     (handler (assoc request
                     :route-params route-params
                     :handler-key handler-key
                     :handler handler-fn)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn wrap-accept [handler]
  (ring.middleware.accept/wrap-accept
    handler
    {:mime
     ["application/json" :qs 1 :as :json
      "application/json-roa+json" :qs 1 :as :json-roa
      "image/apng" :qs 0.8 :as :apng
      "text/css" :qs 1 :as :css
      "text/html" :qs 1 :as :html]}))

(defn wrap-add-vary-header [handler]
  "should be used if content varies based on `Accept` header, e.g. if using `ring.middleware.accept`"
  (fn [request]
    (let [response (handler request)]
      (assoc-in response [:headers "Vary"] "Accept"))))

(defn canonicalize-params-map [params & {:keys [parse-json?]
                                         :or {parse-json? true}}]
  (if-not (map? params)
    params
    (->> params
         (map (fn [[k v]]
                [(keyword k)
                 (if parse-json? (json/try-parse-json v) v)]))
         (into {}))))

(canonicalize-params-map
  {"email" "thomas.schank@zhdk.ch", "password" "secret"} :parse-json? false)

(defn wrap-canonicalize-params-maps [handler]
  (fn [request]
    (handler (-> request
                 (assoc :params-raw
                        (-> request :params-raw
                            (canonicalize-params-map :parse-json? false)))
                 (assoc :query-params-raw
                        (-> request :query-params
                            (canonicalize-params-map :parse-json? false)))
                 (update-in [:params] canonicalize-params-map)
                 (update-in [:query-params] canonicalize-params-map)
                 (update-in [:form-params] canonicalize-params-map)))))

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
  (I> wrap-handler-with-logging
      dispatch-to-handler
      (admin-auth/wrap-authorize skip-authorization-handler-keys)
      wrap-dispatch-content-type
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
      wrap-accept
      wrap-add-vary-header
      wrap-resolve-handler
      wrap-canonicalize-params-maps
      ring.middleware.params/wrap-params
      wrap-content-type
      (wrap-resource
        "public" {:allow-symlinks? true
                  :cache-bust-paths ["/admin/css/site.css"
                                     "/admin/css/site.min.css"
                                     "/admin/js/app.js"]
                  :never-expire-paths [#".*font-awesome-[^\/]*\d\.\d\.\d\/.*"
                                       #".+_[0-9a-f]{40}\..+"]
                  :enabled? (= env/env :prod)})
      shutdown/wrap
      ring-exception/wrap))

;#### debug ###################################################################
;(logging-config/set-logger! :level :debug)
(logging-config/set-logger! :level :info)
;(debug/debug-ns *ns*)
