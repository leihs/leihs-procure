(ns leihs.admin.resources.groups.main
  (:refer-clojure :exclude [str keyword])
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.admin.common.users-and-groups.core :as users-and-groups]
   [leihs.admin.resources.groups.group.main :as group]
   [leihs.admin.resources.groups.shared :as shared]
   [leihs.admin.resources.users.choose-core :as choose-user]
   [leihs.admin.utils.seq :as seq]
   [leihs.core.core :refer [keyword presence str]]
   [next.jdbc.sql :refer [query] :rename {query jdbc-query}]))

(def groups-base-query
  (-> (apply sql/select (map #(keyword (str "groups." %)) shared/default-fields))
      (sql/select [(-> (sql/select :%count.*)
                       (sql/from :groups_users)
                       (sql/where [:= :groups_users.group_id :groups.id]))
                   :count_users])
      (sql/from :groups)
      (sql/order-by :name :id)))

(defn set-per-page-and-offset
  ([query {per-page :per-page page :page}]
   (when (or (-> per-page presence not)
             (-> per-page integer? not)
             (> per-page 1000)
             (< per-page 1))
     (throw (ex-info "The query parameter per-page must be present and set to an integer between 1 and 1000."
                     {:status 422})))
   (when (or (-> page presence not)
             (-> page integer? not)
             (< page 0))
     (throw (ex-info "The query parameter page must be present and set to a positive integer."
                     {:status 422})))
   (set-per-page-and-offset query per-page page))
  ([query per-page page]
   (-> query
       (sql/limit per-page)
       (sql/offset (* per-page (- page 1))))))

(defn term-filter [query request]
  (if-let [term (-> request :query-params-raw :term presence)]
    (-> query
        (sql/where [:or
                    [:% (str term) :groups.searchable]
                    [(keyword "~~*") :groups.searchable (str "%" term "%")]]))
    query))

(defn filter-for-including-user
  [query {{user-uid :including-user} :query-params-raw :as request}]
  (if-let [user-uid (presence user-uid)]
    (sql/where
     query
     [:exists
      (-> (choose-user/find-by-some-uid-query user-uid)
          (sql/select :true)
          (sql/join :groups_users [:= :groups_users.group_id :groups.id])
          (sql/where [:= :groups_users.user_id :users.id]))])
    query))

(defn select-fields [query request]
  (if-let [fields (some->> request :query-params :fields
                           (map keyword) set
                           (clojure.set/intersection shared/available-fields))]
    (apply sql/select query fields)
    query))

(defn groups-query [request]
  (let [query-params (-> request :query-params
                         shared/normalized-query-parameters)]
    (-> groups-base-query
        (set-per-page-and-offset query-params)
        (term-filter request)
        (filter-for-including-user request)
        (users-and-groups/organization-filter request)
        (users-and-groups/org-id-filter request)
        (users-and-groups/protected-filter request)
        (select-fields request))))

(def organizations-query
  (-> (sql/select-distinct :organization)
      (sql/from :groups)))

(defn groups [{tx :tx-next :as request}]
  (let [query (groups-query request)
        offset (:offset query)]
    {:body
     {:meta {:organizations
             (-> organizations-query sql-format
                 (->> (jdbc-query tx) (map :organization)))}
      :groups (-> query
                  sql-format
                  (->> (jdbc-query tx)
                       (seq/with-index offset)
                       seq/with-page-index))}}))

(defn routes [request]
  (case (:request-method request)
    :get (groups request)
    :post (group/create-group request)))

;#### debug ###################################################################
;(debug/debug-ns *ns*)
;(debug/wrap-with-log-debug #'groups-formated-query)


