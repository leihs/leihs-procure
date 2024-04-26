(ns leihs.admin.resources.inventory-pools.inventory-pool.delegations.responsible-user
  (:require
   [honey.sql :refer [format] :rename {format sql-format}]
   [leihs.admin.resources.inventory-pools.inventory-pool.delegations.queries :as queries]
   [logbug.catcher :as catcher]
   [logbug.debug :as debug]
   [next.jdbc.sql :refer [query] :rename {query jdbc-query}]))

(defn find-by-unique-property [unique-id tx]
  (->> unique-id
       queries/find-responsible-user
       sql-format
       (jdbc-query tx)
       first))

(def not-found-ex
  (ex-info
   (str "The responsible user could not be found. "
        "Check the id or email-address.")
   {:status 422}))

;#### debug ###################################################################

;(debug/debug-ns *ns*)
