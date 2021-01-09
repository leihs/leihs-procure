(ns leihs.admin.resources.settings.smtp.main
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.core.core :refer [keyword str presence]])
  (:require
    [leihs.core.sql :as sql]
    [leihs.core.auth.core :as auth]
    [leihs.core.routing.back :as routing :refer [set-per-page-and-offset wrap-mixin-default-query-params]]

    [leihs.admin.resources.users.choose-core :as choose-user]
    [leihs.admin.paths :refer [path]]
    [leihs.admin.resources.audits.requests.shared :refer [default-query-params]]
    [leihs.admin.utils.jdbc :as utils-jdbc]

    [clojure.java.jdbc :as jdbc]
    [compojure.core :as cpj]
    [clojure.string :as string]

    [clj-logging-config.log4j :as logging-config]
    [clojure.tools.logging :as logging]
    [logbug.debug :as debug]
    [logbug.catcher :as catcher]
    ))


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
;(logging-config/set-logger! :level :debug)
;(logging-config/set-logger! :level :info)
;(debug/debug-ns *ns*)
