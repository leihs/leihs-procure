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
         (catch Throwable e*
           (let [e (get-cause e*)
                 m (.getMessage e)
                 n (-> e*
                       .getClass
                       .getSimpleName)]
             (log/warn (or m n))
             (if (env/env #{:dev :test}) (log/debug e))
             (graphql-resolve/resolve-as nil
                                         {:message (str m), ; if message nil
                                          ; convert to ""
                                          :exception n}))))))

(defn- wrap-map-with-error
  [arg]
  (into {} (for [[k v] arg] [k (wrap-resolver-with-error v)])))

(defn resolver-map-fn
  []
  (-> (queries/get-resolver-map)
      (merge (mutations/get-resolver-map))
      wrap-map-with-error))

(def resolver-map (resolver-map-fn))

(defn get-resolver-map
  []
  (if (#{:dev :test} env/env) (resolver-map-fn) resolver-map))

;#### debug ###################################################################
; (logging-config/set-logger! :level :debug)
; (logging-config/set-logger! :level :info)
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
; (debug/undebug-ns *ns*)
