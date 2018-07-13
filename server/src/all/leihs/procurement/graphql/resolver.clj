(ns leihs.procurement.graphql.resolver
  (:require [clojure.tools.logging :as log]
            [com.walmartlabs.lacinia.resolve :as graphql-resolve]
            [leihs.procurement.env :as env]
            [leihs.procurement.graphql [mutations :as mutations]
             [queries :as queries]]
            [leihs.procurement.utils.ring-exception :refer [get-cause]]))

(defn wrap-resolver-with-error
  [resolver]
  (fn [context args value]
    (try (resolver context args value)
         (catch Throwable _e
           (let [e (get-cause _e)
                 m (.getMessage e)
                 n (-> _e
                       .getClass
                       .getSimpleName)]
             (log/warn m)
             (if (env/env #{:dev :test}) (log/debug e))
             (graphql-resolve/resolve-as value {:message m, :exception n}))))))

(defn- wrap-map-with-error
  [arg]
  (into {} (for [[k v] arg] [k (wrap-resolver-with-error v)])))

(defn get-resolver-map
  []
  (-> (queries/query-resolver-map)
      (merge (mutations/mutation-resolver-map))
      wrap-map-with-error))

;#### debug ###################################################################
; (logging-config/set-logger! :level :debug)
; (logging-config/set-logger! :level :info)
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
; (debug/undebug-ns *ns*)
