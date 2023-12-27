(ns leihs.procurement.permissions.user
  (:require
    [honey.sql :refer [format] :rename {format sql-format}]
    [honey.sql.helpers :as sql]
    [leihs.core.core :refer [raise]]
    [leihs.procurement.permissions.categories :as categories-perms]
    [next.jdbc :as jdbc]
    [taoensso.timbre :refer [debug error info spy warn]]))

(defn admin?
  [tx auth-entity]
  (let [
        query (-> (sql/select [[:exists

                                (-> (sql/select [true :exists])
                                    (sql/from :procurement_admins)
                                    (sql/where [:= :procurement_admins.user_id [:cast (:user_id auth-entity) :uuid]]))]])
                  sql-format)]
    (:exists (jdbc/execute-one! tx (spy query)))))

(defn inspector?
  ([tx auth-entity] (inspector? tx auth-entity nil))
  ([tx auth-entity c-id]
   (let [query (-> (sql/select [[:exists
                                 (cond-> (-> (sql/select [true :exists])
                                             (sql/from :procurement_category_inspectors)
                                             (sql/where [:= :procurement_category_inspectors.user_id [:cast (:user_id auth-entity) :uuid]]))
                                         c-id (sql/where [:= :procurement_category_inspectors.category_id [:cast c-id :uuid]]))] :result])
                   sql-format)]
     (:result (jdbc/execute-one! tx (spy query))))))


(defn viewer?
  ([tx auth-entity] (viewer? tx auth-entity nil))
  ([tx auth-entity c-id]
   (let [query (-> (sql/select [[:exists
                                 (cond-> (-> (sql/select [true :exists])
                                             (sql/from :procurement_category_viewers)
                                             (sql/where [:= :procurement_category_viewers.user_id [:cast (:user_id auth-entity) :uuid]]))
                                         c-id (sql/where [:= :procurement_category_viewers.category_id [:cast c-id :uuid]]))] :result])
                   sql-format)]

     (:result (jdbc/execute-one! tx (spy query))))))

(defn requester?
  [tx auth-entity]
  (let [query (-> (sql/select [[:exists
                                (-> (sql/select [true :exists])
                                    (sql/from :procurement_requesters_organizations)
                                    (sql/where [:= :procurement_requesters_organizations.user_id [:cast (:user_id auth-entity) :uuid]]))]])
                  sql-format)]
    (:exists (jdbc/execute-one! tx query))))

(defn advanced?
  [tx auth-entity]
  (->> [viewer? inspector? admin?]
       (map #(% tx auth-entity))
       (some true?)))

(defn get-permissions
  [{{:keys [tx-next authenticated-entity]} :request} args value]
  (when (not= (:user_id authenticated-entity) (:id value))
    (raise "Not allowed to query permissions for a user other then the authenticated one."))
  {:isAdmin (admin? tx-next authenticated-entity),
   :isRequester (requester? tx-next authenticated-entity),
   :isInspectorForCategories (categories-perms/inspected-categories tx-next value),
   :isViewerForCategories (categories-perms/viewed-categories tx-next value)})
