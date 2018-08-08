(ns leihs.procurement.resources.images
  (:require [clojure.java.jdbc :as jdbc]
            [leihs.procurement.utils.sql :as sql]))

(defn delete!
  [tx ids]
  (jdbc/execute! tx
                 (-> (sql/delete-from :procurement_images)
                     (sql/where [:in :procurement_images.id ids])
                     sql/format)))
