(ns leihs.procurement.resources.settings
  (:require [clojure.java.jdbc :as jdbc]
            [leihs.procurement.utils.sql :as sql]))

(def settings-base-query
  (-> (sql/select :procurement_settings.*)
      (sql/from :procurement_settings)))

(defn get-settings
  ([context _ _]
   (get-settings (-> context
                     :request
                     :tx)))
  ([tx]
   (-> settings-base-query
       sql/format
       (->> (jdbc/query tx))
       first)))

(defn update-settings!
  [context args value]
  (let [tx (-> context
               :request
               :tx)
        input-data (:input_data args)
        inspection-comments (->> input-data
                                 :inspection_comments
                                 (map #(sql/call :cast % :text))
                                 (cons :jsonb_build_array)
                                 (apply sql/call))
        settings (-> input-data
                     (assoc :inspection_comments inspection-comments))]
    (jdbc/execute! tx
                   (-> (sql/update :procurement_settings)
                       (sql/sset settings)
                       sql/format))
    (get-settings tx)))
