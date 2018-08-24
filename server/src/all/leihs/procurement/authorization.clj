(ns leihs.procurement.authorization
  (:require [leihs.procurement.env :as env]
            [leihs.procurement.permissions.user :as user-perms]
            [leihs.procurement.graphql.helpers :as helpers])
  (:import leihs.procurement.UnauthorizedException))

(defn wrap-ensure-one-of
  [resolver predicates]
  (fn [context args value]
    (let [rrequest (:request context)
          tx (:tx rrequest)
          auth-entity (:authenticated-entity rrequest)]
      (if (->> predicates
               (map #(% tx auth-entity))
               (some true?))
        (resolver context args value)
        (throw (UnauthorizedException.
                 "Not authorized for this query path and arguments."
                 {}))))))

(defn authorize-and-apply
  [func &
   {:keys [if-only if-any if-all], :or {if-only nil, if-any nil, if-all nil}}]
  {:pre [(if if-only (ifn? if-only) true)
         (if-let [preds (or if-any if-all)]
           (->> preds
                (map ifn?)
                (every? true?))
           true) (= (count (filter nil? [if-only if-any if-all])) 2)]}
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
      (throw (UnauthorizedException.
               "Not authorized for this query path and arguments."
               {})))))

(defn wrap-authorize-resolver
  [resolver check]
  (fn [context args value]
    (authorize-and-apply #(resolver context args value)
                         :if-only
                         #(check context args value))))

(def skip-authorization-handler-keys
  [[:attachment #{:dev :test}] [:image #{:dev :test}] :status
   [:upload #{:dev :test}]])

(defn- skip?
  [handler-key]
  (some #(if (coll? %)
          (and (= (first %) handler-key) (env/env (second %)))
          (= handler-key %))
        skip-authorization-handler-keys))

(defn wrap-authorize
  [handler]
  (fn [request]
    (if (or (skip? (:handler-key request))
            (->> [user-perms/admin? user-perms/inspector? user-perms/viewer?
                  user-perms/requester?]
                 (map #(% (:tx request) (:authenticated-entity request)))
                 (some true?)))
      (handler request)
      {:status 403,
       :body (helpers/error-as-graphql-object
               "NOT_AUTHORIZED_FOR_APP"
               "Not authorized to access procurement!")})))
