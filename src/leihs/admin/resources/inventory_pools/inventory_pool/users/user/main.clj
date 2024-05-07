(ns leihs.admin.resources.inventory-pools.inventory-pool.users.user.main
  (:refer-clojure :exclude [str keyword])
  (:require
   [clojure.set :as set]
   [clojure.set :refer [rename-keys]]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.admin.common.roles.core :as roles]
   [leihs.admin.resources.inventory-pools.inventory-pool.shared :refer [normalized-inventory-pool-id!]]
   [leihs.admin.resources.users.user.main :as root-user]
   [logbug.debug :as debug]
   [next.jdbc :as jdbc]
   [next.jdbc.sql :refer [query] :rename {query jdbc-query, delete! jdbc-delete!}]))

(defn contracts-count [inventory-pool-id state]
  (-> (sql/select :%count.*)
      (sql/from :contracts)
      (sql/where [:= :user_id :users.id])
      (sql/where [:= :inventory_pool_id inventory-pool-id])
      (sql/where [:= :state state])))

(defn reservations-count [inventory-pool-id state]
  (-> (sql/select :%count.*)
      (sql/from :reservations)
      (sql/where [:= :user_id :users.id])
      (sql/where [:= :inventory_pool_id inventory-pool-id])
      (sql/where [:= :status state])))

(defn user-query [inventory-pool-id user-id]
  (-> (root-user/user-query user-id)
      (sql/select [(contracts-count inventory-pool-id "open") :contracts_open_count])
      (sql/select [(contracts-count inventory-pool-id "closed") :contracts_closed_count])
      (sql/select [(reservations-count inventory-pool-id "submitted") :reservations_submitted_count])
      (sql/select [(reservations-count inventory-pool-id "approved") :reservations_approved_count])
      identity))

;;; user ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn user [{{inventory-pool-id :inventory-pool-id
              user-id :user-id} :route-params
             tx :tx :as request}]
  {:body
   (or (->> (-> (user-query inventory-pool-id user-id) sql-format)
            (jdbc-query tx) first)
       (throw (ex-info "User not found" {:status 404})))})

(def routes
  (fn [request]
    (case (:request-method request)
      :get (user request))))

;#### debug ###################################################################

;(debug/wrap-with-log-debug #'filter-suspended)
;(debug/wrap-with-log-debug #'users-formated-query)
;(debug/debug-ns *ns*)
