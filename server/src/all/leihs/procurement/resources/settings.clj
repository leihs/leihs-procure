(ns leihs.procurement.resources.settings
  (:require [clojure.java.jdbc :as jdbc]
            [leihs.procurement.utils.sql :as sql]))

(def settings-base-query
  (-> (sql/select :procurement_settings.*)
      (sql/from :procurement_settings)))

(defn get-settings
  [context _ _]
  (->> settings-base-query
       sql/format
       (jdbc/query (-> context
                       :request
                       :tx))
       first))
