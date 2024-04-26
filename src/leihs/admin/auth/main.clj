(ns leihs.admin.auth.main
  (:refer-clojure :exclude [str keyword])
  (:require
   [clojure.walk]
   [leihs.admin.auth.authorize :as authorize]
   [leihs.admin.paths :refer [path]]
   [leihs.core.core :refer [deep-merge presence str]]
   [pandect.core]))

(defn redirect-target [{{query-target :target} :query-params}]
  (or (presence query-target)
      (path :home)))

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

;(debug/debug-ns *ns*)
