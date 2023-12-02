(ns leihs.procurement.resources.settings
  (:require 
    ;[clojure.java.jdbc :as jdbc]
    ;        [leihs.procurement.utils.sql :as sql]

    [honey.sql :refer [format] :rename {format sql-format}]
    [leihs.core.db :as db]
    [next.jdbc :as jdbc]
    [honey.sql.helpers :as sql]
    
    ))

(def settings-base-query
  (-> (sql/select :procurement_settings.*)
      (sql/from :procurement_settings)))

(defn get-settings
  ([context _ _]
   (get-settings (-> context
                     :request
                     :tx-next)))
  ([tx]
   (-> settings-base-query
       sql-format
       (->> (jdbc/execute-one! tx))
       )))

(defn update-settings!
  [context args value]
  (let [tx (-> context
               :request
               :tx-next)
        input-data (:input_data args)
        inspection-comments (->> input-data
                                 :inspection_comments
                                 (map #( :cast % :text))    ;; TODO FIXME PRIO!!!
                                 (cons :jsonb_build_array)
                                 (apply ))
        settings (-> input-data
                     (assoc :inspection_comments inspection-comments))]
    (jdbc/execute! tx
                   (-> (sql/update :procurement_settings)
                       (sql/set settings)
                       sql-format))
    (get-settings tx)))
