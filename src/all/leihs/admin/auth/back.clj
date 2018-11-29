(ns leihs.admin.auth.back
  (:refer-clojure :exclude [str keyword])
  (:require
    [leihs.core.constants :refer [USER_SESSION_COOKIE_NAME]]
    [leihs.core.core :refer [keyword str presence]]
    [leihs.admin.password-authentication.back :as password-authentication]
    [leihs.core.auth.core :as auth]
    [leihs.core.sql :as sql]

    [leihs.admin.auth.back.authorize :as authorize]
    [leihs.admin.paths :refer [path]]

    [clojure.java.jdbc :as jdbc]
    [clojure.set :refer [rename-keys]]
    [clojure.walk]
    [compojure.core :as cpj]
    [pandect.core]
    [ring.util.response :refer [redirect]]

    [clj-logging-config.log4j :as logging-config]
    [clojure.tools.logging :as logging]
    [logbug.debug :as debug]
    [logbug.catcher :as catcher]
    )
  )

(defn redirect-target [{{query-target :target} :query-params}]
  (or (presence query-target)
      (path :home)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn user-sign-in-base-query [email]
  (-> (sql/select :users.id :is_admin :account_enabled :firstname :lastname :email)
      (sql/from :users)
      (sql/merge-where [:= (sql/call :lower :users.email) (sql/call :lower email)])
      (sql/merge-where [:= :users.account_enabled true])))


(defn password-sign-in [request]
  (let [resp (password-authentication/ring-handler request)]
    (case (:status resp)
      200
      )))

(defn sign-out [request]
  (-> (redirect (path :home {} {:target (redirect-target request)}) :see-other)
      (assoc-in [:cookies (str USER_SESSION_COOKIE_NAME)]
                {:value ""
                 :http-only true
                 :max-age -1
                 :path "/"
                 :secure false})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def routes
  (cpj/routes
    (cpj/POST (path :password-authentication) [] #'password-authentication/ring-handler)
    ; TODO to be removed with legacy (which uses GET to sign out)
    (cpj/GET (path :auth-sign-out) [] #'sign-out)
    (cpj/POST (path :auth-sign-out) [] #'sign-out)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(def HTTP-SAFE-VERBS #{:get :head :options :trace})

(defn http-safe? [request] 
  (boolean (some-> request :request-method HTTP-SAFE-VERBS)))

(def HTTP-UNSAFE-VERBS #{:post :put :delete :patch})

(defn http-unsafe?  [request]
  (boolean (some-> request :request-method HTTP-UNSAFE-VERBS)))


(defn authorize-http-unsafe [request handler required-scopes]
  (cond 
    ; case 1 admin and system_admin write required
    (and (:scope_admin_write required-scopes)
         (:scope_system_admin_write required-scopes)
         (-> request :authenticated-entity :scope_admin_write)
         (-> request :authenticated-entity :scope_system_admin_write))  (handler request)
    ; case 2 system_admin write required
    (and (:scope_system_admin_write required-scopes)
         (-> request :authenticated-entity :scope_system_admin_write))  (handler request)
    ; case 3 admin write required
    (and (:scope_admin_write required-scopes)
         (-> request :authenticated-entity :scope_admin_write))  (handler request)
    ; Note: for now we don't allow neither admin nor system_admin write required because we have not use case so far
    ; all other cases 
    :else {:status 403 :body "No permission to write/modify data!"}))

(defn authorize-http-safe [request handler required-scopes]
  (cond 
    ; case 1 admin and system_admin read required
    (and (:scope_admin_read required-scopes)
         (:scope_system_admin_read required-scopes)
         (-> request :authenticated-entity :scope_admin_read)
         (-> request :authenticated-entity :scope_system_admin_read))  (handler request)
    ; case 2 system_admin read required
    (and (:scope_system_admin_read required-scopes)
         (-> request :authenticated-entity :scope_system_admin_read))  (handler request)
    ; case 3 admin read required
    (and (:scope_admin_read required-scopes)
         (-> request :authenticated-entity :scope_admin_read))  (handler request)
    ; Note: for now we don't allow neither admin nor system_admin read required because we have not use case so far
    ; all other cases 
    :else {:status 403 :body "No permission to read data!"}))

(defn authorize [request handler required-scopes]
  (if (http-unsafe? request)
    (authorize-http-unsafe request handler required-scopes)
    (authorize-http-safe request handler required-scopes)))

(defn wrap-authorize
  ([handler skip-authorization-handler-keys required-scopes]
   (fn [request]
     (wrap-authorize request handler skip-authorization-handler-keys required-scopes)))
  ([handler skip-authorization-handler-keys]
   (fn [request]
     (wrap-authorize request handler skip-authorization-handler-keys 
                           {:scope_admin_read true :scope_admin_write true
                            :scope_system_admin_read false :scope_system_admin_write false})))
  ([request handler skip-authorization-handler-keys required-scopes]
   (if (authorize/handler-is-ignored? skip-authorization-handler-keys request) 
     (handler request)
     (if-not (-> request :authenticated-entity)
       {:status 401 :body "Authentication required!"}
       (authorize request handler required-scopes)))))

     

;#### debug ###################################################################
;(logging-config/set-logger! :level :debug)
;(logging-config/set-logger! :level :info)
;(debug/debug-ns *ns*)
