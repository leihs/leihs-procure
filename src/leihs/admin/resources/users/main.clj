(ns leihs.admin.resources.users.main
  (:refer-clojure :exclude [str keyword])
  (:require
   [bidi.bidi :refer [match-route]]
   [clojure.core.match :refer [match]]
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.admin.common.users-and-groups.core :as users-and-groups]
   [leihs.admin.paths :refer [paths]]
   [leihs.admin.resources.users.queries :as queries]
   [leihs.admin.resources.users.shared :as shared]
   [leihs.admin.resources.users.user.main :as user]
   [leihs.admin.utils.seq :as seq]
   [leihs.core.core :refer [keyword presence str]]
   [leihs.core.routing.back :as routing]
   [next.jdbc.sql :refer [query] :rename {query jdbc-query}]))

(def users-base-query
  (-> (apply sql/select (map #(keyword (str "users." %)) shared/default-fields))
      (sql/from :users)
      (sql/order-by :lastname :firstname :id)
      (sql/where [:= nil :delegator_user_id])))

(defn match-term-with-emails [query term]
  (sql/where
   query
   [:or
    [:= [:lower term] [:lower :users.email]]
    [:= [:lower term] [:lower :users.secondary_email]]]))

(defn match-term-fuzzy [query term]
  (sql/where query [:or
                    [:% (str term) :searchable]
                    [(keyword "~~*") :searchable (str "%" term "%")]]))

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
      (true "true" "yes") (sql/where query [:= true :account_enabled])
      (false "false" "no") (sql/where query [:= false :account_enabled]))))

(defn admin-filter [query request]
  (let [qp  (some-> request :query-params-raw :admin)]
    (case qp
      (nil "any" "") query
      ("leihs-admin") (sql/where query [:= true :is_admin])
      ("system-admin") (sql/where query [:= true :is_system_admin]))))

(defn select-fields [query request]
  (if-let [fields (some->> request :query-params :fields
                           (map keyword) set
                           (clojure.set/intersection shared/available-fields))]
    (apply sql/select query fields)
    query))

(defn select-contract-counts [query]
  (-> query
      (sql/select [queries/open-contracts-sub :open_contracts_count])
      (sql/select [queries/closed-contracts-sub :closed_contracts_count])))

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
        (sql/select [queries/pools-count :pools_count])
        (sql/select [queries/groups-count :groups_count]))))

(def organizations-query
  (-> (sql/select-distinct :organization)
      (sql/from :users)))

(defn users [{tx :tx-next :as request}]
  (let [query (users-query request)
        offset (:offset query)]
    {:body
     {:meta {:organizations
             (-> organizations-query sql-format
                 (->> (jdbc-query tx) (map :organization)))}
      :users
      (-> query
          sql-format
          (->> (jdbc-query tx)
               (seq/with-key :id)
               (seq/with-index offset)
               seq/with-page-index))}}))

(defn routes [request]
  (let [handler-key (->> request :uri (match-route paths) :handler)]
    (match [(:request-method request) handler-key]
      [:get :users] (users request)
      [:get :users-choose] (users request)
      [:post :users] (user/routes request))))

;#### debug ###################################################################

;(debug/debug-ns *ns*)

;(debug/wrap-with-log-debug #'org-filter)
;(debug/wrap-with-log-debug #'users-formated-query)
;(debug/wrap-with-log-debug #'users-formated-query)
