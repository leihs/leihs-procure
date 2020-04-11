(ns leihs.admin.auth.authorization
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.core.core :refer [keyword str presence]])
  (:require
    [clj-logging-config.log4j :as logging-config]
    [clojure.tools.logging :as logging]
    [logbug.debug :as debug]
    [logbug.catcher :as catcher]))



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

(defn scope-authorizer
  ([required-scopes]
   (fn [request]
     (scope-authorizer request required-scopes)))
  ([request required-scopes]
   (let [required-scope-keys (->> required-scopes
                                  (filter (fn [[k v]] v))
                                  (filter-required-scopes-wrt-safe-or-unsafe request)
                                  (map first)
                                  set)]
     (logging/debug 'required-scope-keys required-scope-keys)
     (if (every? (fn [scope-key] (-> request :authenticated-entity scope-key)) required-scope-keys)
       {:allowed? true}
       (let [k (some (fn [scope-key] (-> request :authenticated-entity scope-key not))
                     required-scope-keys)]
         {:allowed? false :reason (str "Permission scope " k " is not satisfied!")})))))

(defn authorize! [request handler resolve-table]
  (let [handler-key (:handler-key request)
        authorizers (some-> resolve-table
                            (get handler-key)
                            :authorizers)]
    (when (nil? authorizers)
      (throw (ex-info (str "No authorizers for handler " handler-key " are defined!")
                      {:status 555})))
    (if (some
          (fn [authorizer] (-> request authorizer :allowed?))
          authorizers)
      (handler request)
      (throw (ex-info "Not authorized" {:status 403})))))

(defn wrap [handler resolve-table]
  (fn [request]
    (authorize! request handler resolve-table)))


;#### debug ###################################################################
;(logging-config/set-logger! :level :debug)
;(logging-config/set-logger! :level :info)
;(debug/debug-ns *ns*)
