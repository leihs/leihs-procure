(ns leihs.procurement.routes
  (:refer-clojure :exclude [str keyword replace])
  (:require
    [clj-logging-config.log4j :as logging-config]
    [leihs.core.http-cache-buster2 :as cache-buster :refer [wrap-resource]]
    [bidi.bidi :as bidi]
    [cheshire.core :refer [parse-string]]
    [clojure.string :refer [starts-with? replace]]
    [clojure.tools.logging :as log]
    [leihs.procurement [authorization :refer [wrap-authenticate wrap-authorize]]
     [env :as env] [graphql :as graphql] [paths :refer [paths]]
     [status :as status]]
    [leihs.core.anti-csrf.back :as anti-csrf]
    [leihs.core.auth.session :as session]
    [leihs.core.locale :as locale]
    [leihs.core.routes :as core-routes]
    [leihs.core.routing.back :as core-routing]
    [leihs.core.settings :as settings]
    [leihs.core.sign-out.back :as sign-out]
    [leihs.procurement.paths :refer [path paths]]
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
    [ring.util.response :refer [redirect status]]
    [clojure.tools.logging :as logging]
    [logbug.debug :as debug :refer [I>]]
    [logbug.ring :refer [wrap-handler-with-logging]]))

(declare redirect-to-root-handler)

(def handler-resolve-table
  (merge core-routes/resolve-table
         {:attachment {:handler attachment/routes},
          :upload {:handler upload/routes},
          :graphql {:handler graphql/handler},
          :home {:handler html/not-found-handler},
          :image {:handler image/routes},
          :not-found {:handler html/not-found-handler},
          :procurement {:handler html/not-found-handler},
          :status {:handler status/routes}}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn redirect-to-root-handler [request] (redirect (path :root)))

(defn handler-resolver
  [handler-key]
  (-> handler-resolve-table
      (get handler-key)
      (#(if (map? %)
          (:handler %)))))

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
    (if (-> matched-pair
            :handler
            (= :not-found))
      (bidi/match-pair paths {:remainder path, :route paths})
      matched-pair)))

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
           (match-pair-with-fallback path)
         handler-fn (handler-resolver handler-key)]
     (handler (assoc request
                :route-params route-params
                :handler-key handler-key
                :handler handler-fn)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn wrap-empty [handler] (fn [request] (or (handler request) {:status 404})))

(defn wrap-secret-byte-array
  "Adds the secret into the request as a byte-array (to prevent
  visibility in logs etc) under the :secret-byte-array key."
  [handler secret]
  (fn [request] (handler (assoc request :secret-ba (.getBytes secret)))))

(defn wrap-rewrite-uri-for-static-paths
  [handler]
  (fn [request]
    (if (and (= (:handler-key request) :not-found))
      (let [uri (:uri request)
            new-uri (some (fn [[s1 s2]]
                            (and (starts-with? uri s1) (replace uri s1 s2)))
                          {"/procure/static" "/procure/client/static",
                           "/procure/theme" "/procure/theme",
                           "/procure/manifest.json"
                             "/procure/client/manifest.json"})]
        (if new-uri (handler (assoc request :uri new-uri)) (handler request)))
      (handler request))))

(defn wrap-accept
  [handler]
  (ring.middleware.accept/wrap-accept
    handler
    {:mime ["application/json" :qs 1 :as :json
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
      session/wrap-authenticate
      wrap-cookies
      settings/wrap
      wrap-json-response
      (wrap-json-body {:keywords? true})
      wrap-empty
      (wrap-secret-byte-array secret)
      datasource/wrap
      core-routing/wrap-canonicalize-params-maps
      wrap-params
      wrap-multipart-params
      wrap-content-type
      (wrap-resource "public"
                     {:allow-symlinks? true,
                      :cache-bust-paths [],
                      :never-expire-paths [],
                      :cache-enabled? true})
      wrap-rewrite-uri-for-static-paths
      wrap-resolve-handler
      wrap-accept
      ring-exception/wrap))

;#### debug ###################################################################
; (logging-config/set-logger! :level :debug)
(logging-config/set-logger! :level :info)
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
