(ns leihs.admin.resources.users.main
  (:refer-clojure :exclude [str keyword])
  (:require
   [clojure.java.jdbc :as jdbc]
   [clojure.set]
   [compojure.core :as cpj]
   [leihs.admin.common.users-and-groups.core :as users-and-groups]
   [leihs.admin.paths :refer [path]]
   [leihs.admin.resources.users.queries :as queries]
   [leihs.admin.resources.users.shared :as shared]
   [leihs.admin.resources.users.user.main :as user]
   [leihs.admin.utils.seq :as seq]
   [leihs.core.core :refer [keyword str presence]]
   [leihs.core.routing.back :as routing :refer []]
   [leihs.core.sql :as sql]
   [logbug.debug :as debug]))

(def users-base-query
  (-> (apply sql/select (map #(keyword (str "users." %)) shared/default-fields))
      (sql/from :users)
      (sql/order-by :lastname :firstname :id)
      (sql/merge-where [:= nil :delegator_user_id])))

(defn match-term-with-emails [query term]
  (sql/merge-where
   query
   [:or
    [:= (sql/call :lower term) (sql/call :lower :users.email)]
    [:= (sql/call :lower term) (sql/call :lower :users.secondary_email)]]))

(defn match-term-fuzzy [query term]
  (sql/merge-where query [:or
                          ["%" (str term) :searchable]
                          ["~~*" :searchable (str "%" term "%")]]))

(defn term-filter [query request]
  (if-let [term (-> request :query-params-raw :term presence)]
    (if (clojure.string/includes? term "@")
      (match-term-with-emails query term)
      (match-term-fuzzy query term))
    query))

(defn account-enabled-filter [query request]
  (let [qp  (some-> request :query-params-raw :account_enabled)]
    (case qp
      (nil "any" "") query
      (true "true" "yes") (sql/merge-where query [:= true :account_enabled])
      (false "false" "no") (sql/merge-where query [:= false :account_enabled]))))

(defn admin-filter [query request]
  (let [qp  (some-> request :query-params-raw :admin)]
    (case qp
      (nil "any" "") query
      ("leihs-admin") (sql/merge-where query [:= true :is_admin])
      ("system-admin") (sql/merge-where query [:= true :is_system_admin]))))

(defn select-fields [query request]
  (if-let [fields (some->> request :query-params :fields
                           (map keyword) set
                           (clojure.set/intersection shared/available-fields))]
    (apply sql/select query fields)
    query))

(defn select-contract-counts [query]
  (-> query
      (sql/merge-select [queries/open-contracts-sub :open_contracts_count])
      (sql/merge-select [queries/closed-contracts-sub :closed_contracts_count])))

(defn users-query [request]
  (let [request (routing/mixin-default-query-params
                 request shared/default-query-params)]
    (-> users-base-query
        (routing/set-per-page-and-offset request)
        (term-filter request)
        (users-and-groups/organization-filter request)
        (users-and-groups/org-id-filter request)
        (users-and-groups/protected-filter request)
        (account-enabled-filter request)
        (admin-filter request)
        (select-fields request)
        select-contract-counts
        (sql/merge-select [queries/pools-count :pools_count])
        (sql/merge-select [queries/groups-count :groups_count]))))

(def organizations-query
  (-> (sql/select :organization)
      (sql/modifiers :distinct)
      (sql/from :users)))

(defn users [{tx :tx :as request}]
  (let [query (users-query request)
        offset (:offset query)]
    {:body
     {:meta {:organizations
             (-> organizations-query sql/format
                 (->> (jdbc/query tx) (map :organization)))}
      :users
      (-> query
          sql/format
          (->> (jdbc/query tx)
               (seq/with-key :id)
               (seq/with-index offset)
               seq/with-page-index))}}))

(def routes
  (cpj/routes
   (cpj/GET (path :users) [] #'users)
   (cpj/GET (path :users-choose) [] #'users)
   (cpj/POST (path :users) [] #'user/routes)))

;#### debug ###################################################################

;(debug/debug-ns *ns*)

;(debug/wrap-with-log-debug #'org-filter)
;(debug/wrap-with-log-debug #'users-formated-query)
;(debug/wrap-with-log-debug #'users-formated-query)
