(ns leihs.admin.resources.system.database.audits.back
  (:refer-clojure :exclude [str keyword])
  (:require
    [leihs.core.core :refer [keyword str presence]]

    [leihs.admin.paths :refer [path]]

    [leihs.core.sql :as sql]
    [leihs.core.ds :as ds]

    [clojure.java.jdbc :as jdbc]
    [clojure.set]
    [compojure.core :as cpj]

    [clj-logging-config.log4j :as logging-config]
    [clojure.tools.logging :as logging]
    [logbug.debug :as debug]
    ))

(defn extend-date-to-iso8601 [s]
  (str s "T00:00:00.000Z"))

(defn legacy-audits-downaload-query [before-date]
  (-> (sql/select :*)
      (sql/from :audits)
      (sql/merge-where [:< :created_at before-date])
      (sql/order-by [:created_at :desc])
      sql/format))

(defn download [{{before-date :before-date} :route-params
                 tx :tx :as request}]
  (let [legacy-audits (->> before-date
                           extend-date-to-iso8601
                           legacy-audits-downaload-query
                           (jdbc/query tx))]
    {:headers {"Content-Disposition" (str "attachment; filename=\"audits_before_" before-date ".json\"")}
     :body {:legacy-audits legacy-audits }}))


(defn delete [{{before-date :before-date} :route-params
               tx :tx :as request}]
  (jdbc/delete! tx :audits ["created_at < ?" (extend-date-to-iso8601 before-date)])
  {:status 204})


(def ^:private before-path (path :database-audits-before {:before-date ":before-date"}))

(def routes
  (cpj/routes
    (cpj/GET before-path [] #'download)
    (cpj/POST before-path [] #'download)
    (cpj/DELETE before-path [] #'delete)))

;#### debug ###################################################################
;(debug/wrap-with-log-debug #'authentication-systems-formated-query)
;(logging-config/set-logger! :level :debug)
;(logging-config/set-logger! :level :info)
;(debug/debug-ns *ns*)


