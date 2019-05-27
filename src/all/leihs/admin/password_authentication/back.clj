(ns leihs.admin.password-authentication.back
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.core.core :refer [keyword str presence]])
  (:require
    [leihs.core.auth.session :as session]
    [leihs.core.password-authentication.back :refer [password-check-query]]
    [leihs.core.constants :refer [PASSWORD_AUTHENTICATION_SYSTEM_ID]]

    [clojure.java.jdbc :as jdbc]
    [clojure.string :as str]))

(defn ring-handler
  [{tx :tx
    {email :email password :password} :body
    settings :settings
    :as request}]
  (if-let [user (->> [email password]
                     (apply password-check-query)
                     (jdbc/query tx)
                     first)]
    (let [user-session (session/create-user-session 
                         user PASSWORD_AUTHENTICATION_SYSTEM_ID request)]
      {:body user
       :status 200
       :cookies {leihs.core.constants/USER_SESSION_COOKIE_NAME
                 {:value (:token user-session)
                  :http-only true
                  :max-age (* 10 356 24 60 60)
                  :path "/"
                  :secure (:sessions_force_secure settings)}}})
    {:status 401
     :body (->> ["Password authentication failed!"
                 "Check your password and try again."
                 "Contact your leihs administrator if the problem persists."]
                (clojure.string/join " \n"))}))
