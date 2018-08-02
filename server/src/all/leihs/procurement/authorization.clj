(ns leihs.procurement.authorization
  (:require [clj-logging-config.log4j :as logging-config]
            [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log]
            [leihs.procurement.permissions.user :as user-perms]
            [leihs.procurement.utils.sql :as sql]
            [logbug.debug :as debug])
  (:import [leihs.procurement UnauthorizedException]))

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

(defn wrap-authorize
  [handler skip-authorization-handler-keys]
  (fn [request]
    (if (or (skip-authorization-handler-keys (:handler-key request))
            (->> [user-perms/admin? user-perms/inspector? user-perms/viewer?
                  user-perms/requester?]
                 (map #(% (:tx request) (:authenticated-entity request)))
                 (some true?)))
      (handler request)
      {:status 403, :body "Not authorized to access procurement!"})))
