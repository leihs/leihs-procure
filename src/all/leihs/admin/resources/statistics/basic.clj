(ns leihs.admin.resources.statistics.basic
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.core.core :refer [keyword str presence]])
  (:require
    [leihs.admin.paths :refer [path]]
    [leihs.core.sql :as sql]

    [clojure.java.jdbc :as jdbc]
    [clojure.set]
    [compojure.core :as cpj]

    [clj-logging-config.log4j :as logging-config]
    [clojure.tools.logging :as logging]
    [logbug.debug :as debug]
    ))


(def now (sql/raw " now() "))
(def one-year-ago (sql/raw " now() - interval '1 years' "))
(def two-years-ago (sql/raw " now() - interval '2 years' "))


(defn active_users_sub [where-cond]
  (-> (sql/select :%count.*)
      (sql/from :users)
      (sql/merge-where
        [:exists
         (-> (sql/select (sql/raw "true"))
             (sql/from :user_sessions)
             (sql/merge-where [:= :user_sessions.user_id :users.id])
             (sql/merge-where where-cond)
             )])))

(def sessions_0m_12m_where_cond
  [:and
   [:<= :user_sessions.created_at now]
   [:> :user_sessions.created_at one-year-ago]])

(def sessions_12m_24m_where_cond
  [:and
   [:<= :user_sessions.created_at one-year-ago]
   [:> :user_sessions.created_at two-years-ago]])


(def active_reservations_0m_12m_cond
  [:or
   [:and
    [:<= :reservations.start_date now]
    [:>  :reservations.start_date one-year-ago]]
   [:and
    [:<= :reservations.returned_date now]
    [:>  :reservations.returned_date two-years-ago]]])

(def active_reservations_12m_24m_cond
  [:or
   [:and
    [:<= :reservations.start_date one-year-ago]
    [:>  :reservations.start_date two-years-ago]]
   [:and
    [:<= :reservations.returned_date one-year-ago]
    [:>  :reservations.returned_date two-years-ago]]])


(defn active_pool_cond [active_reservations_cond]
  [:exists
   (-> (sql/select :1)
       (sql/from :contracts)
       (sql/merge-where [:= :contracts.inventory_pool_id :inventory_pools.id])
       (sql/merge-join :reservations [:= :contracts.id :reservations.contract_id])
       (sql/merge-where active_reservations_cond))])


;;; contracts ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn active_contract_cond [active_reservations_cond]
  [:exists
   (-> (sql/select :1)
       (sql/from :reservations)
       (sql/merge-where [:= :contracts.id :reservations.contract_id])
       (sql/merge-where active_reservations_cond))])

(defn merge-select-contracts [query]
  (-> query
      (sql/merge-select [(-> (sql/select :%count.*)
                             (sql/from :contracts))
                         :contracts_count])
      (sql/merge-select [(-> (sql/select :%count.*)
                             (sql/from :contracts)
                             (sql/merge-where (active_contract_cond
                                                active_reservations_0m_12m_cond)))
                         :active_contracts_0m_12m_count])
      (sql/merge-select [(-> (sql/select :%count.*)
                             (sql/from :contracts)
                             (sql/merge-where (active_contract_cond
                                                active_reservations_12m_24m_cond)))
                         :active_contracts_12m_24m_count])))


;;; items ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn active_item_cond [active_reservations_cond]
  [:exists
   (-> (sql/select :1)
       (sql/from :reservations)
       (sql/merge-where [:= :items.id :reservations.item_id])
       (sql/merge-where active_reservations_cond))])

(defn merge-select-items [query]
  (-> query
      (sql/merge-select [(-> (sql/select :%count.*)
                             (sql/from :items))
                         :items_count])
      (sql/merge-select [(-> (sql/select :%count.*)
                             (sql/from :items)
                             (sql/merge-where (active_item_cond
                                                active_reservations_0m_12m_cond)))
                         :active_items_0m_12m_count])
      (sql/merge-select [(-> (sql/select :%count.*)
                             (sql/from :items)
                             (sql/merge-where (active_item_cond
                                                active_reservations_12m_24m_cond)))
                         :active_items_12m_24m_count])))



;;; models ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn active_model_cond [active_reservations_cond]
  [:exists
   (-> (sql/select :1)
       (sql/from :reservations)
       (sql/merge-join :items [:= :items.id :reservations.item_id])
       (sql/merge-where [:= :models.id :items.model_id])
       (sql/merge-where active_reservations_cond))])

(defn merge-select-models [query]
  (-> query
      (sql/merge-select [(-> (sql/select :%count.*)
                             (sql/from :models))
                         :models_count])
      (sql/merge-select [(-> (sql/select :%count.*)
                             (sql/from :models)
                             (sql/merge-where (active_model_cond
                                                active_reservations_0m_12m_cond)))
                         :active_models_0m_12m_count])
      (sql/merge-select [(-> (sql/select :%count.*)
                             (sql/from :models)
                             (sql/merge-where (active_model_cond
                                                active_reservations_12m_24m_cond)))
                         :active_models_12m_24m_count])))


;;; main ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def query
  (-> (sql/select [(-> (sql/select :%count.*)
                       (sql/from :users)) :users_count])
      (sql/merge-select [(active_users_sub sessions_0m_12m_where_cond)
                         :active_users_0m_12m_count])
      (sql/merge-select [(active_users_sub sessions_12m_24m_where_cond)
                         :active_users_12m_24m_count])

      merge-select-items

      merge-select-models

      merge-select-contracts

      (sql/merge-select [(-> (sql/select :%count.*)
                             (sql/from :inventory_pools)) :pools_count])
      (sql/merge-select [(-> (sql/select :%count.*)
                             (sql/from :inventory_pools)
                             (sql/merge-where (active_pool_cond active_reservations_0m_12m_cond))
                             ) :active_pools_0m_12m_count])
      (sql/merge-select [(-> (sql/select :%count.*)
                             (sql/from :inventory_pools)
                             (sql/merge-where (active_pool_cond active_reservations_12m_24m_cond))
                             ) :active_pools_12m_24m_count])))

(defn routes [{tx :tx :as requests}]
  {:body (->> query sql/format
              (jdbc/query tx) first)} )

;#### debug ###################################################################
;(logging-config/set-logger! :level :debug)
;(logging-config/set-logger! :level :info)
;(debug/debug-ns *ns*)
