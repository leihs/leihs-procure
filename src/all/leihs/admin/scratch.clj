(ns leihs.admin.scratch
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.core.core :refer [keyword str presence]])
  (:require
    [leihs.core.sql :as sql]
    [leihs.core.ds :as ds]

    [clojure.java.jdbc :as jdbc]

    [clj-logging-config.log4j :as logging-config]
    [clojure.tools.logging :as logging]
    [logbug.debug :as debug]
    [logbug.catcher :as catcher]
    ))



(defn add-fkey-wheres [query]
  (reduce
    (fn [query table]
      (sql/merge-where query
                       [:not [:exists (-> (sql/select :true)
                                          (sql/from table)
                                          (sql/merge-where [:= (keyword (str (str table) ".user_id")) :users.id]))]]))
    query
    [:contracts :reservations :entitlement_groups_users :procurement_category_inspectors]))


(defn query-deletable-users []
  (->>
    (-> (sql/select :%count.*)
        (sql/from :users)
        add-fkey-wheres
        (sql/merge-where [:= :users.delegator_user_id nil])
        ;(sql/merge-where (sql/raw "(updated_at < (now() - interval '1 Month'))"))
        sql/format)
    (jdbc/query @ds/ds)))

(defn query-old-closed-contracts-count []
  (->>
    (-> (sql/select :%count.*)
        (sql/from :contracts)
        (sql/merge-where [:= :state "closed"])
        (sql/merge-where (sql/raw "(contracts.updated_at < (now() - interval '2 Years'))"))
        sql/format)
    (jdbc/query @ds/ds)))


(defn query-old-unsubmitted-reservations-count []
  (->>
    (-> (sql/select :%count.*)
        (sql/from :reservations)
        (sql/merge-where [:= :status (sql/raw "'unsubmitted'::reservation_status")])
        (sql/merge-where (sql/raw "(updated_at < (now() - interval '1 Years'))"))
        sql/format)
    (jdbc/query @ds/ds)))

;(->> user-wo-reservations-or-contracts-query (jdbc/query @ds/ds))
