(ns leihs.admin.utils.jdbc
  (:refer-clojure :exclude [str keyword])
  (:require
   [clojure.java.jdbc :as jdbc]
   [leihs.core.core :refer [keyword str presence]]
   [logbug.catcher :as catcher]
   [logbug.debug :as debug]))

(defn insert-or-update! [tx table where-clause values]
  (let [[clause & params] where-clause]
    (if (first (jdbc/query
                tx
                (concat [(str "SELECT 1 FROM " table " WHERE " clause)]
                        params)))
      (jdbc/update! tx table values where-clause)
      (jdbc/insert! tx table values))))

;#### debug ###################################################################

;(debug/debug-ns *ns*)
