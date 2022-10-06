(ns leihs.admin.resources.users.user.core
  (:refer-clojure :exclude [str keyword])
  (:require
    [clojure.string :as string]
    [honey.sql :refer [format] :rename {format sql-format}]
    [honey.sql.helpers :as honey-sql]
    [leihs.admin.utils.regex :refer [uuid-pattern org-id-org-pattern]]
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.sql :as sql]
    [leihs.core.uuid :refer [uuid]]
    ))

(defn sql-merge-unique-user
  "where-merges a unique user condition in and for honeysql1"
  [query uid]
  (cond

    ; case UUID must be the primary ID
    (re-matches uuid-pattern uid)
    (sql/merge-where query [:= :users.id uid])

    ; case ORG_ID and ORG
    (re-matches org-id-org-pattern uid)
    (let [[_ org org_id] (re-matches org-id-org-pattern uid)]
      (sql/merge-where query [:and
                              [:= :users.organization org]
                              [:= :users.org_id org_id]]))

    ; case emails, it suffices to test for @ since the org is out
    ; and login may not contain it
    (clojure.string/includes? uid "@" )
    (sql/merge-where query
                     [:= (sql/call :lower :users.email)
                      (sql/call :lower uid)])


    ; case login
    (and (not (clojure.string/includes? uid "@"))
         (not (clojure.string/includes? uid "|")))
    (sql/merge-where query
                     [:= :users.login uid])


    ; sure nothing matches else
    :else (sql/merge-where query [:= true false])

    ))



(defn sql-where-unique-user
  "adds a unique user where condition in and for honeysql2"
  [query uid]
  (cond

    ; case UUID must be the primary ID
    (re-matches uuid-pattern uid)
    (honey-sql/where query [:= :users.id (uuid uid)])

    ; case ORG_ID and ORG
    (re-matches org-id-org-pattern uid)
    (let [[_ org org_id] (re-matches org-id-org-pattern uid)]
      (honey-sql/where query [:and
                              [:= :users.organization org]
                              [:= :users.org_id org_id]]))

    ; case emails, it suffices to test for @ since the org is out
    ; and login may not contain it
    (clojure.string/includes? uid "@" )
    (honey-sql/where query [:= [:lower :users.email] [:lower uid]])


    ; case login
    (and (not (clojure.string/includes? uid "@"))
         (not (clojure.string/includes? uid "|")))
    (honey-sql/where query [:= :users.login uid])


    ; sure nothing matches else
    :else (honey-sql/where query [:= true false])

    ))
