(ns leihs.admin.resources.inventory-pools.inventory-pool.users.user.main
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.core.core :refer [keyword str presence]])
  (:require
    [leihs.admin.paths :refer [path]]
    [leihs.admin.common.roles.core :as roles]
    [leihs.admin.resources.inventory-pools.inventory-pool.shared :refer [normalized-inventory-pool-id!]]
    [leihs.admin.resources.users.user.main :as root-user]
    [leihs.core.sql :as sql]
    [leihs.admin.utils.jdbc :as utils.jdbc]

    [clojure.java.jdbc :as jdbc]
    [clojure.set :refer [rename-keys]]
    [compojure.core :as cpj]
    [clojure.set :as set]

    [clj-logging-config.log4j :as logging-config]
    [clojure.tools.logging :as logging]
    [logbug.debug :as debug]))


(defn contracts-count [inventory-pool-id state]
  (-> (sql/select :%count.*)
      (sql/from :contracts)
      (sql/merge-where [:= :user_id :users.id])
      (sql/merge-where [:= :inventory_pool_id inventory-pool-id])
      (sql/merge-where [:= :state state])))

(defn reservations-count [inventory-pool-id state]
  (-> (sql/select :%count.*)
      (sql/from :reservations)
      (sql/merge-where [:= :user_id :users.id])
      (sql/merge-where [:= :inventory_pool_id inventory-pool-id])
      (sql/merge-where [:= :status state])))

(defn user-query [inventory-pool-id user-id]
  (-> (root-user/user-query user-id)
      (sql/merge-select [(contracts-count inventory-pool-id "open") :contracts_open_count])
      (sql/merge-select [(contracts-count inventory-pool-id "closed") :contracts_closed_count])
      (sql/merge-select [(reservations-count inventory-pool-id "submitted") :reservations_submitted_count])
      (sql/merge-select [(reservations-count inventory-pool-id "approved") :reservations_approved_count])
      identity))


;;; user ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn user [{{inventory-pool-id :inventory-pool-id
              user-id :user-id } :route-params
             tx :tx :as request}]
  {:body
   (or (->> (-> (user-query inventory-pool-id user-id) sql/format)
            (jdbc/query tx) first)
       (throw (ex-info "User not found" {:status 404})))})


;;; routes ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def inventory-pool-user-path
  (path :inventory-pool-user {:inventory-pool-id ":inventory-pool-id" :user-id ":user-id"}))

(def inventory-pool-users-path
  (path :inventory-pool-users {:inventory-pool-id ":inventory-pool-id"}))

(def routes
  (cpj/routes
    (cpj/GET inventory-pool-user-path [] #'user)))


;#### debug ###################################################################
;(logging-config/set-logger! :level :debug)
;(logging-config/set-logger! :level :info)
;(debug/wrap-with-log-debug #'filter-suspended)
;(debug/wrap-with-log-debug #'users-formated-query)
(debug/debug-ns *ns*)
