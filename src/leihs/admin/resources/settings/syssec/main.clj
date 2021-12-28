(ns leihs.admin.resources.settings.syssec.main
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


    [clojure.tools.logging :as logging]
    [logbug.debug :as debug]
    [logbug.catcher :as catcher]
    ))


(defn get-syssec-settings [{tx :tx}]
  {:body (-> (sql/select :*)
             (sql/from :system_and_security_settings)
             sql/format
             (->> (jdbc/query tx) first)
             (or (throw (ex-info "syssec-settings not found" {:status 404})))
             (dissoc :id))})

(defn upsert [{tx :tx data :body :as request}]
  (utils-jdbc/insert-or-update! tx :system_and_security_settings ["id = 0"] data)
  (get-syssec-settings request))

(def syssec-settings-path (path :syssec-settings {}))

(def routes
  (-> (cpj/routes
        (cpj/GET syssec-settings-path [] #'get-syssec-settings)
        (cpj/PATCH syssec-settings-path [] #'upsert)
        (cpj/PUT syssec-settings-path [] #'upsert))))


;#### debug ###################################################################


;(debug/debug-ns *ns*)
