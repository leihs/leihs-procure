(ns leihs.procurement.routes
  (:refer-clojure :exclude [str keyword])
  (:require
    [leihs.core.http-cache-buster2 :as cache-buster :refer [wrap-resource]]
    [leihs.core.shutdown :as shutdown]

    [bidi.bidi :as bidi]
    [cheshire.core :refer [parse-string]]
    [clojure.tools.logging :as log]
    [leihs.procurement [authorization :refer [wrap-authenticate wrap-authorize]]
     [env :as env] [graphql :as graphql] [paths :refer [paths]]
     [status :as status]]
    [leihs.core.anti-csrf.back :as anti-csrf]
    [leihs.core.locale :as locale]
    [leihs.core.sign-out.back :as sign-out]
    [leihs.procurement.paths :refer [path paths]]
    [leihs.procurement.auth.session :as session]
    [leihs.procurement.backend.html :as html]
    [leihs.procurement.resources [attachment :as attachment] [image :as image]
     [upload :as upload]]
    [leihs.procurement.utils [core :refer [keyword presence]]
     [ds :as datasource] [ring-exception :as ring-exception]]
    [ring-graphql-ui.core :refer [wrap-graphiql]]
    [ring.middleware.content-type :refer [wrap-content-type]]
    [ring.middleware.accept]
    [ring.middleware [cookies :refer [wrap-cookies]]
     [json :refer [wrap-json-body wrap-json-response]]
     [multipart-params :refer [wrap-multipart-params]]
     [params :refer [wrap-params]] [reload :refer [wrap-reload]]]
    [ring.util.response :refer [redirect]]

    [clojure.tools.logging :as logging]
    [logbug.debug :as debug :refer [I>]]
    [logbug.ring :refer [wrap-handler-with-logging]]
    ))

(declare redirect-to-root-handler)

(def handler-resolve-table
  {:attachment attachment/routes
   :upload upload/routes
   :graphql graphql/handler
   :image image/routes
   :not-found html/not-found-handler
   :sign-out sign-out/ring-handler
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

(defn- match-pair-with-fallback
  [path]
  (let [matched-pair (bidi/match-pair paths {:remainder path, :route paths})]
    (if
      (->
        matched-pair
        :handler
        (= :not-found))
      (bidi/match-pair paths {:remainder path, :route paths})
      matched-pair)))

(defn wrap-resolve-handler
  ([handler] (fn [request] (wrap-resolve-handler handler request)))
  ([handler request]
   (let [path
           (or
             (->
               request
               :path-info
               presence)
             (->
               request
               :uri
               presence))
         {route-params :route-params, handler-key :handler}
           (match-pair-with-fallback path)
         handler-fn (handler-resolver handler-key)]
     (handler
       (assoc request
         :route-params route-params
         :handler-key handler-key
         :handler handler-fn)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn canonicalize-params-map
  [params]
  (if-not (map? params)
    params
    (->>
      params
      (map
        (fn [[k v]]
          [(keyword k) (try (parse-string v true) (catch Exception _ v))]))
      (into {}))))

(defn wrap-canonicalize-params-maps
  [handler]
  (fn [request]
    (handler
      (->
        request
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

(defn wrap-dispatch-frontent 
  ([handler]
   (fn [request]
     (wrap-dispatch-frontent handler request)))
  ([handler request]
   (logging/debug 'wrap-dispatch-frontent request)
   (if (and 
         (:handler-key request)
         (= :html (-> request :accept :mime))
         ; some extra logic: when it comes to js browsers don't set the accept header properly
         (not (re-matches #".*\.js$" (:uri request))))
     (let [frontent-request (assoc request 
                                   :uri "/procure/index.html"
                                   :handler-key nil
                                   :handler nil)]
       (logging/debug 'frontent-request frontent-request)
       (handler frontent-request))
     (handler request))))

(defn wrap-accept [handler]
  (ring.middleware.accept/wrap-accept
    handler
    {:mime
     ["application/json" :qs 1 :as :json
      "application/javascript" :qs 1 :as :javascript
      "image/apng" :qs 1 :as :apng
      "image/*" :qs 1 :as :image
      "text/css" :qs 1 :as :css
      "text/html" :qs 1 :as :html]}))

(defn init
  [secret]
  (I> wrap-handler-with-logging
      dispatch-to-handler
      anti-csrf/wrap
      locale/wrap
      wrap-authorize
      wrap-authenticate
      session/wrap
      wrap-cookies
      wrap-json-response
      (wrap-json-body {:keywords? true})
      wrap-empty
      (wrap-secret-byte-array secret)
      datasource/wrap
      (wrap-graphiql {:path "/procure/graphiql", :endpoint "/procure/graphql"})
      wrap-canonicalize-params-maps
      wrap-params
      wrap-multipart-params
      wrap-content-type
      (wrap-resource
        "public" {:allow-symlinks? true
                  :cache-bust-paths []
                  :never-expire-paths []
                  :cache-enabled? true})
      wrap-dispatch-frontent
      wrap-resolve-handler
      wrap-accept
      shutdown/wrap
      ring-exception/wrap
      wrap-reload-if-dev-or-test))

;#### debug ###################################################################
; (logging-config/set-logger! :level :debug)
; (logging-config/set-logger! :level :info)
; (debug/debug-ns 'cider-ci.utils.shutdown)
;(debug/debug-ns *ns*)
