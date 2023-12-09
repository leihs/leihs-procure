(ns leihs.procurement.resources.uploads
  (:require
    ;[clojure.java.jdbc :as jdbc]
    ;        [leihs.procurement.utils.sql :as sql]

    [taoensso.timbre :refer [debug info warn error spy]]

    [honey.sql :refer [format] :rename {format sql-format}]
    [leihs.core.db :as db]
    [next.jdbc :as jdbc]
    [honey.sql.helpers :as sql]

    ))


(defn cast-uuids [uuids]
  (map (fn [uuid-str] [:cast uuid-str :uuid]) uuids))

(defn delete!
  [tx ids]

  (println ">o> >tocheck>")


  (jdbc/execute! tx
                 (-> (sql/delete-from :procurement_uploads)
                     (sql/where [:in :procurement_uploads.id (cast-uuids ids)]) ;; TODO PRIO!!
                     sql-format
                     spy
                     )))
