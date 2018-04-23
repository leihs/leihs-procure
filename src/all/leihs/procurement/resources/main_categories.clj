(ns leihs.procurement.resources.main-categories
  (:require [clj-logging-config.log4j :as logging-config]
            [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as logging]
            [leihs.procurement.paths :refer [path]]
            [leihs.procurement.resources.image :as image]
            [leihs.procurement.utils.sql :as sql]
            [logbug.debug :as debug]))

(def main-categories-base-query
  (-> (sql/select :procurement_main_categories.*)
      (sql/from :procurement_main_categories)))

(defn get-main-categories [context _ _]
  (let [tx (-> context :request :tx)]
    (->> main-categories-base-query
         sql/format
         (jdbc/query tx)
         (map (fn [mc]
                (let [image (->> (:id mc)
                                 image/image-query-for-main-category
                                 sql/format
                                 (jdbc/query tx)
                                 first)]
                  (if-let [image-id (debug/identity-with-logging (:id image))]
                    (merge mc {:image_url (path :image {:image-id image-id})})
                    ""))))
         )))

;#### debug ###################################################################
; (logging-config/set-logger! :level :debug)
; (logging-config/set-logger! :level :info)
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
; (debug/undebug-ns *ns*)
