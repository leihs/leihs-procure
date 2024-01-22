(ns leihs.procurement.permissions.user
  (:require
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.core.core :refer [raise]]
   [leihs.procurement.permissions.categories :as categories-perms]
   [leihs.procurement.utils.helpers :refer [to-uuid]]
   [next.jdbc :as jdbc]
   [taoensso.timbre :refer [debug error info spy warn]]))

(defn admin?
  [tx auth-entity]
  (:exists
   (jdbc/execute-one!
    tx
    (-> (sql/select [[:exists
                      (-> (sql/select [true :exists])
                          (sql/from :procurement_admins)
                          (sql/where [:= :procurement_admins.user_id (:user_id auth-entity)]))]])
        sql-format))))

(defn inspector?
  ([tx auth-entity] (inspector? tx auth-entity nil))
  ([tx auth-entity c-id]
   (:result
    (jdbc/execute-one!
     tx
     (-> (sql/select
          [[:exists
            (cond-> (-> (sql/select [true :exists])
                        (sql/from :procurement_category_inspectors)
                        (sql/where [:= :procurement_category_inspectors.user_id (:user_id auth-entity)]))
              c-id (sql/where [:= :procurement_category_inspectors.category_id (to-uuid c-id)]))] :result])
         sql-format)))))

(defn viewer?
  ([tx auth-entity] (viewer? tx auth-entity nil))
  ([tx auth-entity c-id]
   (:result
    (jdbc/execute-one!
     tx
     (-> (sql/select
          [[:exists
            (cond-> (-> (sql/select [true :exists])
                        (sql/from :procurement_category_viewers)
                        (sql/where [:= :procurement_category_viewers.user_id (:user_id auth-entity)]))
              c-id (sql/where [:= :procurement_category_viewers.category_id (to-uuid c-id)]))] :result])
         sql-format)))))

(defn requester?
  [tx auth-entity]
  (:exists
   (jdbc/execute-one!
    tx
    (-> (sql/select [[:exists
                      (-> (sql/select [true :exists])
                          (sql/from :procurement_requesters_organizations)
                          (sql/where [:= :procurement_requesters_organizations.user_id (:user_id auth-entity)]))]])
        sql-format))))

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
