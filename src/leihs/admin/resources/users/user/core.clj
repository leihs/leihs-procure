(ns leihs.admin.resources.users.user.core
  (:require
   [clojure.string :as string]
   [honey.sql.helpers :as sql]
   [leihs.admin.utils.regex :refer [org-id-org-pattern uuid-pattern]]
   [leihs.core.uuid :refer [uuid] :as uuid]))

(defn sql-merge-unique-user
  "where-merges a unique user condition in and for honeysql1"
  [query uid]
  (cond

    ; case UUID class
    (instance? java.util.UUID uid)
    (sql/where query [:= :users.id uid])

    ; case string which is castable to UUID
    (uuid/castable? uid)
    (sql/where query [:= :users.id (uuid uid)])

    ; case ORG_ID and ORG
    (re-matches org-id-org-pattern uid)
    (let [[_ org org_id] (re-matches org-id-org-pattern uid)]
      (sql/where query [:and
                        [:= :users.organization org]
                        [:= :users.org_id org_id]]))

    ; case emails, it suffices to test for @ since the org is out
    ; and login may not contain it
    (clojure.string/includes? uid "@")
    (sql/where query [:= [:lower :users.email] [:lower uid]])

    ; case login
    (and (not (clojure.string/includes? uid "@"))
         (not (clojure.string/includes? uid "|")))
    (sql/where query [:= :users.login uid])

    ; sure nothing matches else
    :else (sql/where query [:= true false])))

(defn sql-where-unique-user
  "adds a unique user where condition in and for honeysql2"
  [query uid]
  (cond

    ; case UUID must be the primary ID
    (instance? java.util.UUID uid)
    (sql/where query [:= :users.id (uuid uid)])

    ; case ORG_ID and ORG
    (re-matches org-id-org-pattern uid)
    (let [[_ org org_id] (re-matches org-id-org-pattern uid)]
      (sql/where query [:and
                        [:= :users.organization org]
                        [:= :users.org_id org_id]]))

    ; case emails, it suffices to test for @ since the org is out
    ; and login may not contain it
    (clojure.string/includes? uid "@")
    (sql/where query [:= [:lower :users.email] [:lower uid]])

; case login
    (and (not (clojure.string/includes? uid "@"))
         (not (clojure.string/includes? uid "|")))
    (sql/where query [:= :users.login uid])

; sure nothing matches else
    :else (sql/where query [:= true false])))
