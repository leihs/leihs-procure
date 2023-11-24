(ns leihs.procurement.permissions.user
  (:require
    [clojure.java.jdbc :as jdbco]
    [leihs.procurement.utils.sql :as sqlo]

    [honey.sql :refer [format] :rename {format sql-format}]
    [leihs.core.db :as db]
    [next.jdbc :as jdbc]
    [honey.sql.helpers :as sql]

    [clojure.tools.logging :as log]
    [leihs.core.core :refer [raise]]
    [leihs.procurement.permissions.categories :as categories-perms]
    [taoensso.timbre :refer [debug info warn error spy]]))


(defn is-truthy? [b]
  (= true b)
  )

(defn admin? "Returns boolean"
  [tx auth-entity]

  (spy (-> (sql/select [true :has_entry])
      (sql/from :procurement_admins)
      (sql/where [:= :user_id [:cast (:user_id auth-entity) :uuid]])
      sql-format))

  (-> (sql/select [true :has_entry])
      (sql/from :procurement_admins)
      (sql/where [:= :user_id [:cast (:user_id auth-entity) :uuid]])

      sql-format

      (->> (jdbc/execute-one! tx))
      (:has_entry)
      (is-truthy?))

  )

(defn inspector?
  ([tx auth-entity] (inspector? tx auth-entity nil))
  ([tx auth-entity c-id]

   (let [
         has-entry (-> (sql/select [true :has_entry])
                       (sql/from :procurement_category_inspectors)
                       (sql/where
                         [:= :user_id
                          (:user_id auth-entity)]))

         res (cond-> has-entry
                     c-id (sql/where [:= :category_id c-id]))

         query (sql-format res)

         res (jdbc/execute-one! tx (spy query))
         ]
     (is-truthy? (:has_entry res))
     )
   ))

(defn viewer?
  ([tx auth-entity] (viewer? tx auth-entity nil))
  ([tx auth-entity c-id]                                    ;;FIXME

   (let [
         has-entry (-> (sql/select [true :has_entry])
                       (sql/from :procurement_category_viewers)
                       (sql/where
                         [:= :user_id
                          (:user_id auth-entity)]))

         res (cond-> has-entry
                     c-id (sql/where [:= :category_id c-id]))

         query (sql-format res)

         res (jdbc/execute-one! tx (spy query))
         ]
     (is-truthy? (:has_entry res))
     )))

(defn requester?
  [tx auth-entity]

  (spy(-> (sql/select [true :has_entry])
      (sql/from :procurement_requesters_organizations)
      (sql/where [:= :user_id [:cast (:user_id auth-entity) :uuid]])
      sql-format))

  (-> (sql/select [true :has_entry])
      (sql/from :procurement_requesters_organizations)
      (sql/where [:= :user_id [:cast (:user_id auth-entity) :uuid]])

      ;(spy sql-format)
      sql-format

      ;(spy)
      (->> (jdbc/execute-one! tx))
      (:has_entry)
      (is-truthy?))
  )

(defn advanced?
  [tx auth-entity]
  (spy (->> [viewer? inspector? admin?]
            (map #(% tx auth-entity))
            (some true?))))

(defn get-permissions
  [{{:keys [tx-next authenticated-entity]} :request} args value]
  ;[{{:keys [tx-next tx authenticated-entity]} :request} args value]

  (when (not= (:user_id authenticated-entity) (:id value))
    (raise "Not allowed to query permissions for a user other then the authenticated one."))
  {:isAdmin (admin? tx-next authenticated-entity),
   :isRequester (requester? tx-next authenticated-entity),
   :isInspectorForCategories (categories-perms/inspected-categories tx-next value),
   :isViewerForCategories (categories-perms/viewed-categories tx-next value)})
