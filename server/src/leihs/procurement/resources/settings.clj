(ns leihs.procurement.resources.settings
  (:require
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [next.jdbc :as jdbc]
   [taoensso.timbre :refer [debug error info spy warn]]))

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
       sql-format
       (->> (jdbc/execute-one! tx)))))

(defn update-settings!
  [context args value]
  (let [tx (-> context
               :request
               :tx)
        input-data (:input_data args)
        inspection-comments (->> input-data
                                 :inspection_comments
                                 (map (fn [comment] [:cast comment :text]))
                                 (cons :jsonb_build_array))
        settings (-> input-data
                     (assoc :inspection_comments inspection-comments))]
    (jdbc/execute-one! tx (-> (sql/update :procurement_settings)
                              (sql/set settings)
                              sql-format))
    (get-settings tx)))
