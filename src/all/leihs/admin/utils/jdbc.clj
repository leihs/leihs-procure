(ns leihs.admin.utils.jdbc
  (:require
    [leihs.admin.utils.core :refer [keyword str presence]]
    [clojure.java.jdbc :as jdbc]

    [clojure.tools.logging :as logging]
    [logbug.catcher :as catcher]
    [logbug.debug :as debug]
    ))

(defn insert-or-update! [tx table where-clause values]
  (let [[clause & params] where-clause]
    (if (first (jdbc/query
                 tx
                 (concat [(str "SELECT 1 FROM " table " WHERE " clause)]
                         params)))
      (jdbc/update! tx table values where-clause)
      (jdbc/insert! tx table values))))

;#### debug ###################################################################
;(logging-config/set-logger! :level :debug)
;(logging-config/set-logger! :level :info)
;(debug/debug-ns 'cider-ci.utils.shutdown)
(debug/debug-ns *ns*)
