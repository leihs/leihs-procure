(ns leihs.admin.auth.back
  (:refer-clojure :exclude [str keyword])
  (:require
    [leihs.core.constants :refer [USER_SESSION_COOKIE_NAME]]
    [leihs.core.core :refer [keyword str presence deep-merge]]
    [leihs.admin.password-authentication.back :as password-authentication]
    [leihs.core.auth.core :as auth]
    [leihs.core.sql :as sql]

    [leihs.admin.auth.back.authorize :as authorize]
    [leihs.admin.paths :refer [path]]

    [clojure.java.jdbc :as jdbc]
    [clojure.set :refer [rename-keys subset?]]
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


(defn filter-required-scopes-wrt-safe-or-unsafe [request required-scopes]
  (if (http-safe? request)
    (filter (fn [[k v]]
              (#{:scope_read :scope_system_admin_read :scope_admin_read} k)) 
            required-scopes)
    required-scopes))


(defn authorize-scope [request handler required-scopes]
  (let [required-scope-keys (->> required-scopes  
                                 (filter (fn [[k v]] v)) 
                                 (filter-required-scopes-wrt-safe-or-unsafe request)
                                 (map first) 
                                 set)]
    (if (every? (fn [scope-key] (-> request :authenticated-entity scope-key)) 
                required-scope-keys) 
      (handler request)
      (let [k (some (fn [scope-key] (-> request :authenticated-entity scope-key not)) 
                    required-scope-keys)]
        {:status 403 :body (str "Permission scope " k " is not satisfied!")}))))

(def default-auth-opts 
  {:skip-authorization-handler-keys #{}
   :required-scopes {:scope_admin_read true 
                     :scope_admin_write true
                     :scope_system_admin_read false 
                     :scope_system_admin_write false}})


(defn authorize [request handler auth-opts]
  (let [normalized-auth-opts (deep-merge default-auth-opts auth-opts)
        skip-authorization-handler-keys (:skip-authorization-handler-keys normalized-auth-opts)
        required-scopes (:required-scopes normalized-auth-opts)]
    (cond
      (authorize/handler-is-ignored? 
        skip-authorization-handler-keys request) (handler request)
      (-> request :authenticated-entity not) {:status 401 :body "Authentication required!"}
      :else (authorize-scope request handler required-scopes))))

(defn wrap-authorize
  ([handler auth-opts]
   "Checks authorization continues with handler if satisfied or returns a 403 otherwise.
   Considers also the http verb wrt save or unsafe, 
   i.g. {:required-scopes {:scope_system_admin_write true}} will be be removed if the action is safe!"
   (fn [request]
     (authorize request handler auth-opts))))

;#### debug ###################################################################
;(logging-config/set-logger! :level :debug)
;(logging-config/set-logger! :level :info)
;(debug/debug-ns *ns*)
