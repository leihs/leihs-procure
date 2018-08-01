(ns leihs.procurement.resources.uploads
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log]
            [leihs.procurement.utils.sql :as sql]))

(defn delete!
  [tx ids]
  (jdbc/execute! tx
                 (-> (sql/delete-from :procurement_uploads)
                     (sql/where [:in :procurement_uploads.id ids])
                     sql/format)))
