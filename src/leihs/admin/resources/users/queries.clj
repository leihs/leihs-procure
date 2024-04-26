(ns leihs.admin.resources.users.queries
  (:require [honey.sql.helpers :as sql]))

(def open-contracts-sub
  (-> (sql/select :%count.*)
      (sql/from :contracts)
      (sql/where [:= :contracts.user_id :users.id])
      (sql/where [:= :state "open"])))

(def closed-contracts-sub
  (-> (sql/select :%count.*)
      (sql/from :contracts)
      (sql/where [:= :contracts.user_id :users.id])
      (sql/where [:= :state "closed"])))

(def pools-count
  (-> (sql/select :%count.*)
      (sql/from :access_rights)
      (sql/where [:= :access_rights.user_id :users.id])))

(def groups-count
  (-> (sql/select :%count.*)
      (sql/from :groups_users)
      (sql/where [:= :groups_users.user_id :users.id])))
