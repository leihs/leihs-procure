(ns leihs.procurement.authorization
  (:require [clj-logging-config.log4j :as logging-config]
            [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log]
            [leihs.procurement.resources.user :as user]
            [leihs.procurement.utils.sql :as sql]
            [logbug.debug :as debug])
  (:import [leihs.procurement UnauthorizedException]))

(defn ensure-one-of
  [resolver predicates]
  (fn [context args value]
    (let [request (:request context)
          tx (:tx request)
          auth-user (:authenticated-entity request)]
      (if (->> predicates
               (map #(% tx auth-user))
               (some true?))
        (resolver context args value)
        (throw (UnauthorizedException. "Not authorized for this query path."
                                       {}))))))

(defn authorize-and-apply
  [tx func auth-user & {:keys [only any all], :or {only nil, any nil, all nil}}]
  {:pre [(= (count (filter nil? [only any all])) 2)]} ; one and only one of
  ; keyword params must be
  ; provided
  (let [auth-func (cond only #(only tx auth-user)
                        any (fn []
                              (->> any
                                   (map #(% tx auth-user))
                                   (some true?)))
                        all (fn []
                              (->> all
                                   (map #(% tx auth-user))
                                   (every? true?))))]
    (if (auth-func)
      (func)
      (throw (UnauthorizedException. "Not authorized for this query path."
                                     {})))))

(defn wrap-authorize
  [handler skip-authorization-handler-keys]
  (fn [request]
    (if (or (log/spy (skip-authorization-handler-keys (:handler-key request)))
            (log/spy
              (->> [user/admin? user/inspector? user/viewer? user/requester?]
                   (map #(% (:tx request) (:authenticated-entity request)))
                   (some true?))))
      (handler request)
      {:status 403, :body "Not authorized to access procurement!"})))
