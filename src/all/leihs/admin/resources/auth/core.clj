(ns leihs.admin.resources.auth.core
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.admin.utils.core :refer [keyword str presence]])
  (:require
    [leihs.admin.constants :refer [USER_SESSION_COOKIE_NAME]]
    [leihs.admin.paths :refer [path]]
    [leihs.admin.resources.auth.back.authorize :as authorize]
    [leihs.admin.resources.auth.back.session :as session]
    [leihs.admin.resources.auth.back.system-admin :as system-admin]
    [leihs.admin.resources.auth.back.token :as token]
    [leihs.admin.utils.sql :as sql]

    [cider-ci.open-session.encryptor :as encryptor]
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

(defn user-sign-in-base-query [email]
  (-> (sql/select :users.id :is_admin :sign_in_enabled :firstname :lastname :email)
      (sql/from :users)
      (sql/merge-where [:= (sql/call :lower :users.email) (sql/call :lower email)])
      (sql/merge-where [:= :users.sign_in_enabled true])))

(defn pw-matches-clause [pw]
  (sql/call
    := :users.pw_hash
    (sql/call :crypt pw :users.pw_hash)))

(defn password-sign-in-query [email password secret]
  (-> (user-sign-in-base-query email)
      (sql/merge-join :settings [:= :settings.id 0])
      (sql/merge-where [:or
                        (pw-matches-clause password)
                        [:and
                         [:= :settings.accept_server_secret_as_universal_password true]
                         [:= password secret] ]])
      (sql/merge-where [:= :users.password_sign_in_enabled true])
      sql/format))

(defn password-sign-in
  ([{{email :email password :password url :url} :form-params tx :tx sba :secret-ba}]
   (password-sign-in email password url (String. sba) tx))
  ([email password url secret tx]
   (if-let [user (->> (password-sign-in-query email password secret)
                      (jdbc/query tx) first)]
     (session/create-user-session
       user secret (redirect (or (-> url presence)
                                 (path :admin)) :see-other) tx)
     (redirect (path :admin {} {:sign-in-warning true})
               :see-other))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn shib-params->user-params [m]
  (let [m (clojure.walk/keywordize-keys m)]
    {:email (:mail m)
     :firstname (:givenname m)
     :lastname (:surname m)
     :org_id (:uniqueid m)}))

(defn validate-user-params! [params]
  (doseq [p [:email]]
    (when-not (presence (get params p nil))
      (throw (ex-info (str "The parameter " p " is required to sign in!")
                      {:status 412 :params params})))))

(defn shib-sign-in-query [{email :email}]
  (-> (user-sign-in-base-query email)
      sql/format))

(defn shib-sign-in
  ([{headers :headers tx :tx sba :secret-ba settings :settings}]
   (shib-sign-in headers (String. sba) tx settings))
  ([headers secret tx settings]
   (when-not (:shibboleth_enabled settings)
     (throw (ex-info "Shibboleth sign-in is disabled." {:status 403})))
   (let [user-params (shib-params->user-params headers)
         _ (validate-user-params! user-params)]
     (if-let [user (->> (shib-sign-in-query user-params)
                        (jdbc/query tx) first)]
       (session/create-user-session
         user secret (redirect (path :admin) :see-other)  tx)
       (redirect (path :admin {} {:sign-in-warning true})
                 :see-other)))))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn shibbsession-cookie-name [request]
  (->> request
       :cookies
       keys
       (filter #(.contains % "_shibsession_"))
       first))

(defn sign-out [request]
  (-> (redirect (or (-> request :form-params :url presence)
                    (path :admin)) :see-other)
      (assoc-in [:cookies (str USER_SESSION_COOKIE_NAME)]
                {:value ""
                 :http-only true
                 :max-age -1
                 :path "/"
                 :secure false})
      (assoc-in [:cookies (or (shibbsession-cookie-name request)
                              "bogus")]
                {:value ""
                 :http-only true
                 :max-age -1
                 :path "/"
                 :secure false})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-auth [request]
  (when (= :json (-> request :accept :mime))
    (when-let [auth-ent (:authenticated-entity request)]
      {:body auth-ent})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def routes
  (cpj/routes
    (cpj/GET (path :auth) [] #'get-auth)
    (cpj/GET (path :auth-shib-sign-in) [] #'shib-sign-in)
    (cpj/POST (path :auth-password-sign-in) [] #'password-sign-in)
    (cpj/POST (path :auth-sign-out) [] #'sign-out)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn wrap-authenticate [handler]
  (-> handler
      token/wrap
      session/wrap
      system-admin/wrap))

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
;(debug/debug-ns 'cider-ci.utils.shutdown)
(debug/debug-ns *ns*)
