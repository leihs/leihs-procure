(ns leihs.procurement.resources.uploads
  (:require
    ;[clojure.java.jdbc :as jdbc]
    ;        [leihs.procurement.utils.sql :as sql]

    [honey.sql :refer [format] :rename {format sql-format}]
    [leihs.core.db :as db]
    [next.jdbc :as jdbc]
    [honey.sql.helpers :as sql]

    ))

(defn delete!
  [tx ids]
  (jdbc/execute! tx
                 (-> (sql/delete-from :procurement_uploads)
                     (sql/where [:in :procurement_uploads.id ids])
                     sql-format)))
