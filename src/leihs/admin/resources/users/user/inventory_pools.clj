(ns leihs.admin.resources.users.user.inventory-pools
  (:require
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.admin.resources.users.user.core :refer [sql-merge-unique-user]]
   [next.jdbc.sql :refer [query] :rename {query jdbc-query}]))

(def contracts-count
  (-> (sql/select :%count.*)
      (sql/from :contracts)
      (sql/where [:= :contracts.inventory_pool_id :inventory-pools.id])
      (sql/where [:= :contracts.user_id :users.id])))

(def open-contracts-count
  (-> (sql/select :%count.*)
      (sql/from :contracts)
      (sql/where [:= :contracts.user_id :users.id])
      (sql/where [:= :contracts.inventory_pool_id :inventory-pools.id])
      (sql/where [:= :contracts.state "open"])))

(defn reservations-count [& {:keys [stati]
                             :or {stati ["unsubmitted"
                                         "submitted"
                                         "approved"
                                         "rejected"
                                         "closed"
                                         "signed"]}}]
  (-> (sql/select :%count.*)
      (sql/from :reservations)
      (sql/where [:= :reservations.inventory_pool_id :inventory-pools.id])
      (sql/where [:= :reservations.user_id :users.id])
      (sql/where [:in :reservations.status stati])))

(defn user-inventory-pools-query [uid]
  (-> (sql/select :access_rights.role
                  [:inventory_pools.name :inventory_pool_name]
                  [:inventory_pools.id :inventory_pool_id])
      (sql/from :users)
      (sql-merge-unique-user uid)
      (sql/join :access_rights [:= :users.id :access_rights.user_id])
      (sql/join :inventory_pools [:= :access_rights.inventory_pool_id :inventory_pools.id])
      (sql/select [open-contracts-count :open_contracts_count])
      (sql/select [contracts-count :contracts_count])
      (sql/select [(reservations-count :stati ["submitted"]) :submitted_reservations_count])
      (sql/select [(reservations-count :stati ["approved"]) :approved_reservations_count])
      (sql/select [(reservations-count) :reservations_count])
      sql-format))

(defn inventory-pools [uid tx]
  (->> uid
       user-inventory-pools-query
       (jdbc-query tx)))

(defn user-inventory-pools
  [{tx :tx data :body {uid :user-id} :route-params}]
  {:body {:user-inventory-pools (inventory-pools uid tx)}})

;;; create user ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn routes [request]
  (case (:request-method request)
    :get (user-inventory-pools request)))

;#### debug ###################################################################

;(debug/wrap-with-log-debug #'data-url-img->buffered-image)
;(debug/wrap-with-log-debug #'buffered-image->data-url-img)
;(debug/wrap-with-log-debug #'resized-img)

;(debug/debug-ns *ns*)
