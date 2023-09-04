(ns leihs.admin.resources.settings.smtp.main
  (:refer-clojure :exclude [str keyword])
  (:require
    [clojure.java.jdbc :as jdbc]
    [clojure.string :as string]
    [compojure.core :as cpj]
    [leihs.admin.paths :refer [path]]
    [leihs.admin.resources.audits.requests.shared :refer [default-query-params]]
    [leihs.admin.utils.jdbc :as utils-jdbc]
    [leihs.core.auth.core :as auth]
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.routing.back :as routing :refer [set-per-page-and-offset wrap-mixin-default-query-params]]
    [leihs.core.sql :as sql]
    [logbug.catcher :as catcher]
    [logbug.debug :as debug]))


(defn get-smtp-settings [{tx :tx}]
  {:body (-> (sql/select :*)
             (sql/from :smtp_settings)
             sql/format
             (->> (jdbc/query tx) first)
             (or (throw (ex-info "smtp-settings not found" {:status 404})))
             (dissoc :id))})

(defn upsert [{tx :tx data :body :as request}]
  (utils-jdbc/insert-or-update! tx :smtp_settings ["id = 0"] data)
  (get-smtp-settings request))

(def smtp-settings-path (path :smtp-settings {}))

(def routes
  (-> (cpj/routes
        (cpj/GET smtp-settings-path [] #'get-smtp-settings)
        (cpj/PATCH smtp-settings-path [] #'upsert)
        (cpj/PUT smtp-settings-path [] #'upsert))))


;#### debug ###################################################################


;(debug/debug-ns *ns*)
