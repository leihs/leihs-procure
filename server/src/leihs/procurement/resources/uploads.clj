(ns leihs.procurement.resources.uploads
  (:require
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [next.jdbc :as jdbc]
   [taoensso.timbre :refer [debug error info spy warn]]))

(defn delete!
  [tx ids]
  (jdbc/execute! tx
                 (-> (sql/delete-from :procurement_uploads)
                     (sql/where [:in :procurement_uploads.id ids])
                     sql-format)))
