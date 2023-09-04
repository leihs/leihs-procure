(ns leihs.admin.resources.users.queries
  (:refer-clojure :exclude [str keyword])
  (:require
    [clojure.java.jdbc :as jdbc]
    [clojure.set]
    [compojure.core :as cpj]
    [leihs.admin.paths :refer [path]]
    [leihs.admin.resources.users.shared :as shared]
    [leihs.admin.resources.users.user.main :as user]
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.sql :as sql]
    [logbug.debug :as debug]))

(def open-contracts-sub
  (-> (sql/select :%count.*)
      (sql/from :contracts)
      (sql/merge-where [:= :contracts.user_id :users.id])
      (sql/merge-where [:= :state "open"])))

(def closed-contracts-sub
  (-> (sql/select :%count.*)
      (sql/from :contracts)
      (sql/merge-where [:= :contracts.user_id :users.id])
      (sql/merge-where [:= :state "closed"])))

(def pools-count
  (-> (sql/select :%count.*)
      (sql/from :access_rights)
      (sql/merge-where [:= :access_rights.user_id :users.id])))

(def groups-count
  (-> (sql/select :%count.*)
      (sql/from :groups_users)
      (sql/merge-where [:= :groups_users.user_id :users.id])))
