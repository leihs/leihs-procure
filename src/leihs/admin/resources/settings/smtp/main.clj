(ns leihs.admin.resources.settings.smtp.main
  (:require
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.admin.paths :refer [path]]
   [leihs.admin.utils.jdbc :as utils-jdbc]
   [next.jdbc.sql :refer [query] :rename {query jdbc-query}]))

(defn get-smtp-settings [{tx :tx}]
  {:body (-> (sql/select :*)
             (sql/from :smtp_settings)
             sql-format
             (->> (jdbc-query tx) first)
             (or (throw (ex-info "smtp-settings not found" {:status 404})))
             (dissoc :id))})

(defn upsert [{tx :tx data :body :as request}]
  (utils-jdbc/insert-or-update! tx :smtp_settings ["id = 0"] data)
  (-> (get-smtp-settings request) (assoc :status 200)))

(def smtp-settings-path (path :smtp-settings {}))

(defn routes [request]
  (case (:request-method request)
    :get (get-smtp-settings request)
    :patch (upsert request)
    :put (upsert request)))

;#### debug ###################################################################

;(debug/debug-ns *ns*)
