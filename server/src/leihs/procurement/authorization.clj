(ns leihs.procurement.authorization
  (:require
    [clojure.tools.logging :as log]
    [leihs.core.core :refer [presence]]
    [leihs.core.sign-in.external-authentication.back :as ext-auth]
    [leihs.procurement.graphql.helpers :as helpers]
    [leihs.procurement.paths :refer [path]]
    [leihs.procurement.permissions.user :as user-perms]

    [taoensso.timbre :refer [debug info warn error spy]]

    [logbug.debug :as debug :refer [I>]]
    [ring.util.response :as response]))


(defn myp [name var]
  (println ">myprint> " name var)
  var
  )

(defn throw-unauthorized []
  (throw (ex-info
           (str "UnauthorizedException"
                " - " "Not authorized for this query path and arguments.")
           {:status 403})))

(defn wrap-ensure-one-of
  [resolver predicates]
  (fn [context args value]
    (let [rrequest (:request context)
          tx (:tx-next rrequest)
          p (println ">>1x" tx)
          auth-entity (:authenticated-entity rrequest)
          p (println ">>2x" auth-entity)
          ]
      (if (->> predicates
               (map #(% tx auth-entity))
               (some true?))
        (resolver context args value)
        (throw-unauthorized)
        ))))

(defn authorize-and-apply
  [func &
   {:keys [if-only if-any if-all], :or {if-only nil, if-any nil, if-all nil}}]
  {:pre [(if if-only (ifn? if-only) true)
         (if-let [preds (or if-any if-all)]
           (->> preds
                (map ifn?)
                (every? true?))
           true)
         (= (count (filter nil? [if-only if-any if-all])) 2)]}
  (let [auth-func (cond if-only if-only
                        if-any (fn []
                                 (->> if-any
                                      (map #(%))
                                      (some true?)))
                        if-all (fn []
                                 (->> if-all
                                      (map #(%))
                                      (every? true?))))]
    (if (auth-func)
      (func)
      (throw-unauthorized))))

(defn wrap-authorize-resolver
  [resolver check]
  (fn [context args value]
    (authorize-and-apply #(resolver context args value)
                         :if-only
                         #(check context args value))))

(def skip-authorization-handler-keys
  (clojure.set/union #{:attachment
                       :image
                       :sign-in
                       :status
                       :upload}
                     ext-auth/skip-authorization-handler-keys))

(defn- skip?
  [handler-key]
  (some #(= handler-key %) skip-authorization-handler-keys))

(defn authenticate [handler {:keys [uri query-string handler-key] :as request}]

  (println ">>> authenticate::skip=" (skip? handler-key) " handler-key=" handler-key)
  (println ">>> authenticate::authenticated-entity" (:authenticated-entity request))

  (cond
    (or (skip? handler-key) (:authenticated-entity request))
    (handler request)
    (= handler-key :graphql)
    {:status 401,
     :body (helpers/error-as-graphql-object "NOT_AUTHENTICATED22"
                                            "Not authenticated!")}
    :else
    (response/redirect
      (path :sign-in
            nil
            {:return-to (cond-> uri
                                (presence query-string)
                                (str "?" query-string))}))))

(defn wrap-authenticate
  [handler]
  (fn [request]
    ;(println ">>wrap-authenticate::handler" handler)
    ;(println ">>wrap-authenticate::request" request)

    (authenticate handler request)))



(defn authorize [handler request]
  (println "\n>> authorize >")
  (println "\n>> authorize::handler >" handler)
  (println "\n>> authorize::request >" request)
  (println "\n>> authorize::handler-key >>" (:handler-key request))

  (let [

        auth-ent (:authenticated-entity request)
        p (myp "authEnt?" auth-ent)
        txn (:tx request)
        p (myp "txn" txn)

        p (myp "admin" (user-perms/admin? txn auth-ent))
        p (myp "inspector" (user-perms/inspector? txn auth-ent))
        p (myp "viewer" (user-perms/viewer? txn auth-ent))
        p (myp "requester" (user-perms/requester? txn auth-ent))
        ]
    )

  ;(if (or (spy (skip? (:handler-key request)))
  ;        (spy (->> [user-perms/admin? user-perms/inspector? user-perms/viewer?
  ;                   user-perms/requester?]
  ;                  (map #(% (:tx-next request) (:authenticated-entity request)))
  ;                  (some true?)))

  (if (or  (skip? (:handler-key request))
          (->> [user-perms/admin? user-perms/inspector? user-perms/viewer?
                     user-perms/requester?]
                    (map #(% (:tx-next request) (:authenticated-entity request)))
                    (some true?))
          )
    (handler request)
    {:status 403,
     :body (helpers/error-as-graphql-object
             "NOT_AUTHORIZED_FOR_APP"
             "Not authorized to access procurement!")}))

(defn wrap-authorize
  [handler]
  (fn [request]
    (authorize handler request)))
