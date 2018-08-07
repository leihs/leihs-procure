(ns leihs.procurement.handler
  (:refer-clojure :exclude [str keyword])
  (:require [bidi.bidi :as bidi]
            [cheshire.core :refer [parse-string]]
            [leihs.procurement [authorization :refer [wrap-authorize]]
             [env :as env] [graphql :as graphql] [paths :refer [path paths]]
             [status :as status]]
            [leihs.procurement.anti-csrf.core :as anti-csrf]
            [leihs.procurement.auth.session :as session]
            [leihs.procurement.backend.html :as html]
            [leihs.procurement.resources [attachment :as attachment]
             [image :as image] [upload :as upload]]
            [leihs.procurement.utils [core :refer [keyword presence]]
             [ds :as datasource] [ring-exception :as ring-exception]]
            [ring-graphql-ui.core :refer [wrap-graphiql]]
            [ring.middleware [cookies :refer [wrap-cookies]]
             [json :refer [wrap-json-body wrap-json-response]]
             [multipart-params :refer [wrap-multipart-params]]
             [params :refer [wrap-params]] [reload :refer [wrap-reload]]]
            [ring.util.response :refer [redirect]]))

(declare redirect-to-root-handler)

(def handler-resolve-table
  {:attachment attachment/routes,
   :upload upload/routes,
   :graphql graphql/handler,
   :image image/routes,
   :not-found html/not-found-handler,
   :status status/routes})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn redirect-to-root-handler [request] (redirect (path :root)))

(defn handler-resolver
  [handler-key]
  (get handler-resolve-table handler-key nil))

(defn dispatch-to-handler
  [request]
  (if-let [handler (:handler request)]
    (handler request)
    (throw
      (ex-info
        "There is no handler for this resource and the accepted content type."
        {:status 404, :uri (get request :uri)}))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn wrap-resolve-handler
  ([handler] (fn [request] (wrap-resolve-handler handler request)))
  ([handler request]
   (let [path (or (-> request
                      :path-info
                      presence)
                  (-> request
                      :uri
                      presence))
         {route-params :route-params, handler-key :handler}
           (bidi/match-pair paths {:remainder path, :route paths})
         handler-fn (handler-resolver handler-key)]
     (handler (assoc request
                :route-params route-params
                :handler-key handler-key
                :handler handler-fn)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn canonicalize-params-map
  [params]
  (if-not (map? params)
    params
    (->> params
         (map (fn [[k v]] [(keyword k)
                           (try (parse-string v true) (catch Exception _ v))]))
         (into {}))))

(defn wrap-canonicalize-params-maps
  [handler]
  (fn [request]
    (handler (-> request
                 (update-in [:params] canonicalize-params-map)
                 (update-in [:query-params] canonicalize-params-map)
                 (update-in [:form-params] canonicalize-params-map)))))

(defn wrap-empty [handler] (fn [request] (or (handler request) {:status 404})))

(defn wrap-secret-byte-array
  "Adds the secret into the request as a byte-array (to prevent
  visibility in logs etc) under the :secret-byte-array key."
  [handler secret]
  (fn [request] (handler (assoc request :secret-ba (.getBytes secret)))))

(defn wrap-reload-if-dev-or-test
  [handler]
  (cond-> handler
    (#{:dev :test} env/env) (wrap-reload {:dirs ["src" "resources"]})))

(defn init
  [secret]
  (-> dispatch-to-handler
      anti-csrf/wrap
      wrap-authorize
      session/wrap
      wrap-cookies
      wrap-json-response
      (wrap-json-body {:keywords? true})
      wrap-empty
      (wrap-secret-byte-array secret)
      datasource/wrap
      wrap-resolve-handler
      (wrap-graphiql {:path "/procure/graphiql", :endpoint "/procure/graphql"})
      wrap-canonicalize-params-maps
      wrap-params
      wrap-multipart-params
      ring-exception/wrap
      wrap-reload-if-dev-or-test))

;#### debug ###################################################################
; (logging-config/set-logger! :level :debug)
; (logging-config/set-logger! :level :info)
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
