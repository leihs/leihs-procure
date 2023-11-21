(ns leihs.procurement.resources.images
  (:require
    [honey.sql :refer [format] :rename {format sql-format}]
    [honey.sql.helpers :as sql]
    [leihs.procurement.utils.helpers :refer [cast-uuids]]
    [next.jdbc :as jdbc]))

(defn delete!
  [tx ids]
  (jdbc/execute! tx (-> (sql/delete-from :procurement_images)
                        (sql/where [:in :procurement_images.id (cast-uuids ids)])
                        sql-format)))
