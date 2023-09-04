(ns leihs.admin.resources.settings.languages.main
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
    [leihs.core.json :refer [to-json]]
    [leihs.core.routing.back :as routing :refer [set-per-page-and-offset wrap-mixin-default-query-params]]
    [leihs.core.sql :as sql]
    [logbug.catcher :as catcher]
    [logbug.debug :as debug]))

(defn get-languages-settings [{tx :tx}]
  {:body
   (-> (sql/select :*)
       (sql/from :languages)
       (sql/order-by :locale)
       sql/format
       (->> (jdbc/query tx)
            (map (fn [l]  [(:locale l) l]))
            (into {}))
       (or (throw (ex-info "no languages found" {:status 404}))))})

(defn put [{tx :tx data :body :as request}]
  (doseq [[locale lang] data]
    (-> (sql/update :languages)
        (sql/set (->> (select-keys lang [:default :active])
                      (map (fn [[k v]] [(keyword (str "\"" k "\"")) v]))
                      (into {})))
        (sql/merge-where [:= :languages.locale (str locale)])
        sql/format
        (->> (jdbc/execute! tx))))
  (get-languages-settings request))

(def languages-settings-path (path :languages-settings {}))

(def routes
  (-> (cpj/routes
        (cpj/GET languages-settings-path [] #'get-languages-settings)
        (cpj/PUT languages-settings-path [] #'put))))


;#### debug ###################################################################


;(debug/debug-ns *ns*)
