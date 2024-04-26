(ns leihs.admin.utils.jdbc
  (:refer-clojure :exclude [str keyword])
  (:require
   [leihs.core.core :refer [str]]
   [next.jdbc.sql :refer [insert! query update!] :rename {query jdbc-query,
                                                          insert! jdbc-insert!,
                                                          update! jdbc-update!}]))

(defn insert-or-update! [tx table where-clause values]
  (let [[clause & params] where-clause]
    (if (first (jdbc-query
                tx
                (concat [(str "SELECT 1 FROM " table " WHERE " clause)]
                        params)))
      (jdbc-update! tx table values where-clause)
      (jdbc-insert! tx table values))))

;#### debug ###################################################################

;(debug/debug-ns *ns*)
