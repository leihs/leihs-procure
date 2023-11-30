(ns leihs.admin.auth.authorize
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.core.core :refer [keyword str presence]])
  (:require
   [logbug.catcher :as catcher]
   [logbug.debug :as debug]))

(def HTTP-SAFE-VERBS #{:get :head :options :trace})
(def HTTP-UNSAFE-VERBS #{:post :put :delete :patch})

(defn is-not-admin? [request]
  (-> request :authenticated-entity :is_admin not))

(defn handler-is-ignored? [ignore-handler-keys request]
  (boolean (when-let [handler-key (:handler-key request)]
             (handler-key ignore-handler-keys))))

(defn authenticated-entity-not-present? [request]
  (not (contains? request :authenticated-entity)))

(defn violates-admin-write-scope? [request]
  (boolean
   (and ((:request-method request)
         #{:post :put :delete :patch})
        (-> request :authenticated-entity
            :scope_admin_write not))))

(defn admin-and-safe? [request]
  (boolean
   (and ((:request-method request) HTTP-SAFE-VERBS)
        (-> request :authenticated-entity :is_admin))))

(defn admin-write-scope-and-unsafe? [request]
  (boolean
   (and ((:request-method request) HTTP-UNSAFE-VERBS)
        (-> request :authenticated-entity :is_admin))))




