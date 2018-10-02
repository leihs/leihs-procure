(ns leihs.admin.auth.back
  (:refer-clojure :exclude [str keyword])
  (:require
    [leihs.core.constants :refer [USER_SESSION_COOKIE_NAME]]
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.password-authentication.back :as password-authentication]
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

(defn wrap-authorize
  ([handler skip-authorization-handler-keys ]
   (fn [request]
     (wrap-authorize request handler skip-authorization-handler-keys)))
  ([request handler skip-authorization-handler-keys]
   (cond
     (authorize/handler-is-ignored?
       skip-authorization-handler-keys request) (handler request)
     (authorize/admin-and-safe?
       request) (handler request)
     (authorize/admin-write-scope-and-unsafe?
       request) (handler request)
     (authorize/authenticated-entity-not-present?
       request) {:status 401
                 :body "Authentication required!"}
     (authorize/is-not-admin?
       request) {:status 403
                 :body "Only for admins!"}
     (authorize/violates-admin-write-scope?
       request) {:status 403
                 :body "No permission to write/modify data!"}
     :else {:status 500
            :body "Authorization check is not implement error!"})))

;#### debug ###################################################################
;(logging-config/set-logger! :level :debug)
;(logging-config/set-logger! :level :info)
;(debug/debug-ns *ns*)
