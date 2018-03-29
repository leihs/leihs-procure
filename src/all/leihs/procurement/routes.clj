(ns leihs.procurement.routes
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.procurement.utils.core :refer [keyword str presence]])
  (:require
    [leihs.procurement.anti-csrf.core :as anti-csrf]
    [leihs.procurement.backend.html :as html]
    [leihs.procurement.constants :as constants]
    [leihs.procurement.env :as env]
    [leihs.procurement.graphql :as graphql]
    [leihs.procurement.paths :refer [path paths]]
    [leihs.procurement.utils.ds :as ds]
    [leihs.procurement.utils.http-resources-cache-buster :as cache-buster :refer [wrap-resource]]
    [leihs.procurement.utils.json-protocol]
    [leihs.procurement.utils.ring-exception :as ring-exception]

    [bidi.bidi :as bidi]
    [bidi.ring :refer [make-handler]]
    [cheshire.core :as json]
    [compojure.core :as cpj]
    [ring.middleware.accept]
    [ring.middleware.content-type :refer [wrap-content-type]]
    [ring.middleware.cookies]
    [ring.middleware.json]
    [ring.middleware.params]
    [ring.util.response :refer [redirect]]
    [ring-graphql-ui.core :refer [wrap-graphiql]]

    [clj-logging-config.log4j :as logging-config]
    [clojure.tools.logging :as logging]
    [logbug.catcher :as catcher]
    [logbug.debug :as debug :refer [I>]]
    [logbug.ring :refer [wrap-handler-with-logging]]
    [logbug.thrown :as thrown]
    ))

(declare redirect-to-root-handler)

(def skip-authorization-handler-keys
  #{:auth-shib-sign-in
    :auth-password-sign-in
    :initial-admin
    :status})

(def do-not-dispatch-to-std-frontend-handler-keys
  #{
    :redirect-to-root 
    :not-found 
    :auth-shib-sign-in})

(def handler-resolve-table
  {
   :graphql graphql/handler

   ; :auth auth/routes
   ; :auth-password-sign-in auth/routes
   ; :auth-shib-sign-in auth/routes
   ; :auth-sign-out auth/routes
   ; :not-found html/not-found-handler
   ; :redirect-to-root redirect-to-root-handler
   })



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn redirect-to-root-handler [request]
  (redirect (path :root)))

(defn handler-resolver [handler-key]
  (get handler-resolve-table handler-key nil))

(defn dispatch-to-handler [request]
  (if-let [handler (:handler request)]
    (handler request)
    (throw 
      (ex-info 
        "There is no handler for this resource and the accepted content type."
        {:status 404}))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn wrap-dispatch-content-type
  ([handler]
   (fn [request]
     (wrap-dispatch-content-type handler request)))
  ([handler request]
   (cond
     ; accept json always goes to the backend handlers, i.e. the normal routing
     (= (-> request :accept :mime) :json) (or (handler request)
                                              (throw (ex-info "This resource does not provide a json response."
                                                              {:status 406})))
     ; accept HTML and GET (or HEAD) wants allmost always the frontend
     (and (= (-> request :accept :mime) :html)
          (#{:get :head} (:request-method request))
          (not (do-not-dispatch-to-std-frontend-handler-keys
                 (:handler-key request)))) (html/html-handler request)
     ; other request might need to go the backend and return frontend nevertheless
     :else (let [response (handler request)]
             (if (and (nil? response)
                      ; TODO we might not need the following after we check (?nil response)
                      (not (do-not-dispatch-to-std-frontend-handler-keys
                             (:handler-key request)))
                      (not (#{:post :put :patch :delete} (:request-method request)))
                      (= (-> request :accept :mime) :html))
               (html/html-handler request)
               response)))))

(defn wrap-resolve-handler
  ([handler]
   (fn [request]
     (wrap-resolve-handler handler request)))
  ([handler request]
   (let [path (or (-> request :path-info presence) (-> request :uri presence))
         {route-params :route-params handler-key :handler} (bidi/match-pair paths {:remainder path
                                                                                   :route paths})
         handler-fn (handler-resolver handler-key)]
     (handler (assoc request
                     :route-params route-params
                     :handler-key handler-key
                     :handler handler-fn)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn wrap-accept [handler]
  (ring.middleware.accept/wrap-accept
    handler
    {:mime
     ["application/json-roa+json" :qs 1 :as :json-roa
      "application/json" :qs 1 :as :json
      "text/html" :qs 1 :as :html ]}))

(defn canonicalize-params-map [params]
  (if-not (map? params)
    params
    (->> params
         (map (fn [[k v]]
                [(keyword k)
                 (try (json/parse-string v true)
                      (catch Exception _ v))]))
         (into {}))))

(defn wrap-canonicalize-params-maps [handler]
  (fn [request]
    (handler (-> request
                 (update-in [:params] canonicalize-params-map)
                 (update-in [:query-params] canonicalize-params-map)
                 (update-in [:form-params] canonicalize-params-map)))))

(defn wrap-empty [handler]
  (fn [request]
    (or (handler request)
        {:status 404})))

(defn wrap-secret-byte-array
  "Adds the secret into the request as a byte-array (to prevent
  visibility in logs etc) under the :secret-byte-array key."
  [handler secret]
  (fn [request]
    (handler (assoc request :secret-ba (.getBytes secret)))))

(defn init [secret]
  (I> wrap-handler-with-logging
      dispatch-to-handler
      ; (auth/wrap-authorize skip-authorization-handler-keys)
      wrap-dispatch-content-type
      ring.middleware.json/wrap-json-response
      (ring.middleware.json/wrap-json-body {:keywords? true})
      anti-csrf/wrap
      ; auth/wrap-authenticate
      ring.middleware.cookies/wrap-cookies
      wrap-empty
      ring-exception/wrap
      (wrap-secret-byte-array secret)
      ; initial-admin/wrap
      ; settings/wrap
      ds/wrap-tx
      wrap-accept
      wrap-resolve-handler
      (wrap-graphiql {:path "/procure/graphiql" :endpoint "/procure/graphql"})
      wrap-canonicalize-params-maps
      ring.middleware.params/wrap-params
      (wrap-resource
        "public" {:allow-symlinks? true
                  :cache-bust-paths ["/admin/css/site.css"
                                     "/admin/css/site.min.css"
                                     "/admin/js/app.js"]
                  :never-expire-paths [#".*font-awesome-[^\/]*\d\.\d\.\d\/.*"
                                       #".+_[0-9a-f]{40}\..+"]
                  :enabled? (= env/env :prod)})
      wrap-content-type
      ring-exception/wrap))

;#### debug ###################################################################
; (logging-config/set-logger! :level :debug)
;(logging-config/set-logger! :level :info)
;(debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
