(ns leihs.admin.resources.settings.languages.main
  (:refer-clojure :exclude [str keyword])
  (:require
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.core.core :refer [keyword str]]
   [next.jdbc :as jdbc]
   [next.jdbc.sql :refer [query] :rename {query jdbc-query}]))

(defn get-languages-settings [{tx :tx-next}]
  {:body
   (-> (sql/select :*)
       (sql/from :languages)
       (sql/order-by :locale)
       sql-format
       (->> (jdbc-query tx)
            (map (fn [l]  [(:locale l) l]))
            (into {}))
       (or (throw (ex-info "no languages found" {:status 404}))))})

(defn put [{tx :tx-next data :body :as request}]
  (doseq [[locale lang] data]
    (-> (sql/update :languages)
        (sql/set (->> (select-keys lang [:default :active])
                      (map (fn [[k v]] [(name k) v]))
                      (into {})))
        (sql/where [:= :languages.locale (str locale)])
        sql-format
        (->> (jdbc/execute! tx))))
  (-> (get-languages-settings request) (assoc :status 200)))

(defn routes [request]
  (case (:request-method request)
    :get (get-languages-settings request)
    :put (put request)))

;#### debug ###################################################################

;(debug/debug-ns *ns*)
