(ns leihs.procurement.permissions.user
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log]
            [leihs.core.core :refer [raise]]
            [leihs.procurement.permissions.categories :as categories-perms]
            [leihs.procurement.utils.sql :as sql]
            [taoensso.timbre :refer [debug info warn error spy]]))

(defn admin?
  [tx auth-entity]
  (spy (:result
    (spy (first
      (jdbc/query
        tx
        (-> (sql/select
              [(sql/call :exists
                         (-> (sql/select true)
                             (sql/from :procurement_admins)
                             (sql/where [:= :procurement_admins.user_id
                                         (:user_id auth-entity)]))) :result])
            sql/format)))))))

(defn inspector?
  ([tx auth-entity] (inspector? tx auth-entity nil))
  ([tx auth-entity c-id]
   (spy (:result
     (spy (first
       (jdbc/query
         tx
         (-> (sql/select
               [(sql/call
                  :exists
                  (cond-> (-> (sql/select true)
                              (sql/from :procurement_category_inspectors)
                              (sql/merge-where
                                [:= :procurement_category_inspectors.user_id
                                 (:user_id auth-entity)]))
                          c-id (sql/merge-where
                                 [:= :procurement_category_inspectors.category_id
                                  c-id]))) :result])
             sql/format))))))))

(defn viewer?
  ([tx auth-entity] (viewer? tx auth-entity nil))
  ([tx auth-entity c-id]
   (spy (:result
     (spy (first (jdbc/query
              tx
              (-> (sql/select
                    [(sql/call
                       :exists
                       (cond-> (-> (sql/select true)
                                   (sql/from :procurement_category_viewers)
                                   (sql/merge-where
                                     [:= :procurement_category_viewers.user_id
                                      (:user_id auth-entity)]))
                               c-id (sql/merge-where
                                      [:= :procurement_category_viewers.category_id
                                       c-id]))) :result])
                  sql/format))))))))

(defn requester?
  [tx auth-entity]
  (spy (:result
    (spy (first
      (jdbc/query
        tx
        (-> (sql/select
              [(sql/call :exists
                         (-> (sql/select true)
                             (sql/from :procurement_requesters_organizations)
                             (sql/where
                               [:= :procurement_requesters_organizations.user_id
                                (:user_id auth-entity)]))) :result])
            sql/format)))))))

(defn advanced?
  [tx auth-entity]
  (spy (->> [viewer? inspector? admin?]
       (map #(% tx auth-entity))
       (some true?))))

(defn get-permissions
  [{{:keys [tx authenticated-entity]} :request} args value]
  (when (not= (:user_id authenticated-entity) (:id value))
    (raise "Not allowed to query permissions for a user other then the authenticated one."))
  {:isAdmin (admin? tx authenticated-entity),
   :isRequester (requester? tx authenticated-entity),
   :isInspectorForCategories (categories-perms/inspected-categories tx value),
   :isViewerForCategories (categories-perms/viewed-categories tx value)})
