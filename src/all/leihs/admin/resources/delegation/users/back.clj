(ns leihs.admin.resources.delegation.users.back
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.admin.utils.core :refer [keyword str presence]])
  (:require
    [leihs.admin.paths :refer [path]]
    [leihs.admin.resources.delegation.users.shared :refer [delegation-users-filter-value]]
    [leihs.admin.resources.users.back :as users]
    [leihs.admin.utils.sql :as sql]

    [clojure.java.jdbc :as jdbc]
    [compojure.core :as cpj]


    [clojure.tools.logging :as logging]
    [logbug.debug :as debug]
    ))


(defn users-query [{:as request
                    {delegation-id :delegation-id} :route-params}]
  (let [query (-> request users/users-query
                  (sql/merge-left-join :delegations_users 
                                       [:and 
                                        [:= :delegations_users.user_id :users.id]
                                        [:= :delegations_users.delegation_id delegation-id]])
                  (sql/merge-select [:delegations_users.delegation_id :delegation_id]))]
    (if-not (-> request :query-params delegation-users-filter-value)
      query
      (-> query
          (sql/merge-where 
            [:= :delegations_users.delegation_id delegation-id])))))


(defn delegation-users-count-query [{{delegation-id :delegation-id} :route-params}]
  (-> (sql/select :%count.*)
      (sql/from :delegations_users)
      (sql/merge-where 
        [:= :delegations_users.delegation_id delegation-id])
      (sql/format)))
  
(defn users-formated-query [request]
  (-> request
      users-query
      sql/format))

(defn users [{tx :tx :as request}]
  {:body
   {:delegation_users_count (->> (delegation-users-count-query request)
                                 (jdbc/query tx)
                                 first :count)
    :users (->> (users-formated-query request)
                (jdbc/query tx))}})

; TODO this is not idempotent 
(defn add-user [{tx :tx :as request
                    {delegation-id :delegation-id 
                     user-id :user-id} :route-params}]
  (if (= 1 (->> {:delegation_id delegation-id
                 :user_id user-id}
                (jdbc/insert! tx :delegations_users )
                count))
    {:status 204}
    (throw (ex-info "Add delegation-user failed" {:request request}))))

(defn remove-user [{tx :tx :as request
                    {delegation-id :delegation-id 
                     user-id :user-id} :route-params}]
  (if (= 1 (->> ["delegation_id = ? AND user_id = ?" delegation-id user-id]
                (jdbc/delete! tx :delegations_users)
                first))
    {:status 204}
    (throw (ex-info "Remove delegation-user failed" {:request request}))))
  
(def delegation-user-path
  (path :delegation-user {:delegation-id ":delegation-id" :user-id ":user-id"}))

(def routes
  (cpj/routes
    (cpj/PUT delegation-user-path [] #'add-user)
    (cpj/DELETE delegation-user-path [] #'remove-user)
    (cpj/GET (path :delegation-users {:delegation-id ":delegation-id"}) [] #'users)))


;#### debug ###################################################################
;(logging-config/set-logger! :level :debug)
;(logging-config/set-logger! :level :info)
;(debug/debug-ns 'cider-ci.utils.shutdown)
;(debug/debug-ns *ns*)
