(ns leihs.admin.resources.inventory-pools.inventory-pool.delegations.responsible-user
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.core.core :refer [keyword str presence]])
  (:require
    [leihs.core.sql :as sql]
    [leihs.admin.resources.inventory-pools.inventory-pool.delegations.queries :as queries]

    [clojure.java.jdbc :as jdbc]


    [clojure.tools.logging :as logging]
    [logbug.debug :as debug]
    [logbug.catcher :as catcher]
    ))

(defn find-by-unique-property [unique-id tx]
  (->> unique-id
       queries/find-responsible-user
       sql/format
       (jdbc/query tx)
       first))

(def not-found-ex
  (ex-info
    (str "The responsible user could not be found. "
         "Check the id or email-address.")
    {:status 422}))

;#### debug ###################################################################


;(debug/debug-ns *ns*)
