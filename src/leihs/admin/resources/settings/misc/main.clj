(ns leihs.admin.resources.settings.misc.main
  (:require
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.admin.paths :refer [path]]
   [leihs.admin.utils.jdbc :as utils-jdbc]
   [next.jdbc.sql :refer [query] :rename {query jdbc-query}]
   [taoensso.timbre :refer [debug]]))

(defn get-misc-settings [{tx :tx}]
  {:body (-> (sql/select :*)
             (sql/from :settings)
             sql-format
             (->> (jdbc-query tx) first)
             (or (throw (ex-info "misc-settings not found" {:status 404})))
             (dissoc :id))})

(defn upsert [{tx :tx data :body :as request}]
  (-> data
      (dissoc :updated_at :created_at)
      (->> (utils-jdbc/insert-or-update! tx :settings ["id = 0"])))
  (-> (get-misc-settings request) (assoc :status 200)))

(defn routes [request]
  (case (:request-method request)
    :get (get-misc-settings request)
    :patch (upsert request)
    :put (upsert request)))

;#### debug ###################################################################

;(debug/debug-ns *ns*)
