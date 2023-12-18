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

(defn admin? "Returns boolean"
  [tx auth-entity]


  (let [
        query (-> (sql/select [[:exists

                                (-> (sql/select [true :exists])
                                    (sql/from :procurement_admins)
                                    (sql/where [:= :procurement_admins.user_id [:cast (:user_id auth-entity) :uuid]]))

                                ]]
                              )
                  sql-format
                  spy
                  )

        p (println ">o> admin?" query)

        result (jdbc/execute-one! tx (spy query))
        p (println ">o> admin?" (spy result))
        ]

    (spy (:exists result))
    )
  )

(defn inspector?
  ([tx auth-entity] (inspector? tx auth-entity nil))
  ([tx auth-entity c-id]


   (let [
         query (-> (sql/select [[:exists
                                 (cond-> (-> (sql/select [true :exists])
                                             (sql/from :procurement_category_inspectors)
                                             (sql/where [:= :procurement_category_inspectors.user_id [:cast (:user_id auth-entity) :uuid]]))
                                         c-id (sql/where [:= :procurement_category_inspectors.category_id [:cast c-id :uuid]]))] :result])
                   sql-format)


         p (println ">o> inspector?" query)

         result (jdbc/execute-one! tx (spy query))
         p (println ">o> inspector?" (spy result))
         ]

     (spy (:result result))

     )))


(defn viewer?
  ([tx auth-entity] (viewer? tx auth-entity nil))
  ([tx auth-entity c-id]                                    ;;FIXME broken??

   (let [
         query (-> (sql/select [[:exists
                                 (cond-> (-> (sql/select [true :exists])
                                             (sql/from :procurement_category_viewers)
                                             (sql/where [:= :procurement_category_viewers.user_id [:cast (:user_id auth-entity) :uuid]]))
                                         c-id (sql/where [:= :procurement_category_viewers.category_id [:cast c-id :uuid]]))] :result])
                   sql-format)

         p (println ">o> viewer?" query)

         result (jdbc/execute-one! tx (spy query))
         p (println ">o> viewer?" (spy result))
         ]

     (spy (:result result))
     )))


(comment

  (let [
        user_id #uuid "b363936a-fd9c-567a-82a4-f0b523dbb26b"
        c-id #uuid "80226a51-c17a-5fd8-93db-40aef5b03491"

        auth-entity {:user_id user_id}

        tx (db/get-ds-next)

        ;sql (cond-> (-> (sql/select [true :exists])
        ;                (sql/from :procurement_category_viewers)
        ;                (sql/where [:= :procurement_category_viewers.user_id [:cast (:user_id auth-entity) :uuid]]))
        ;            c-id (sql/where [:= :procurement_category_viewers.category_id [:cast c-id :uuid]]))



        ;sql  (-> (sql/select [true :exists])
        ;                (sql/from :procurement_category_viewers)
        ;                (sql/where [:= :procurement_category_viewers.user_id [:cast (:user_id auth-entity) :uuid]])
        ;                ;(sql/where [:= :procurement_category_viewers.user_id [:cast user_id :uuid]])
        ;         sql-format
        ;         )

        sql (-> (sql/select [[:exists
                              (cond-> (-> (sql/select [true :exists])
                                          (sql/from :procurement_category_viewers)
                                          (sql/where [:= :procurement_category_viewers.user_id [:cast (:user_id auth-entity) :uuid]]))
                                      c-id (sql/where [:= :procurement_category_viewers.category_id [:cast c-id :uuid]]))] :result])
                sql-format)


        p (println "\nquery" sql)

        result (jdbc/execute-one! tx sql)

        p (println "\result" (:result result))



        ;sql (-> (sql/select [[:exists
        ;                      ;(cond-> (-> (sql/select true)
        ;                      (spy (cond-> (-> (sql/select [true :exists])
        ;                                       (sql/from :procurement_category_viewers)
        ;                                       (sql/where [:= :procurement_category_viewers.user_id [:cast (:user_id auth-entity) :uuid]]))
        ;                                   c-id (sql/where [:= :procurement_category_viewers.category_id [:cast c-id :uuid]]))) :result]])
        ;        sql-format)
        ;
        ;p (println "\nquery" sql)
        ;
        ;
        ;result (jdbc/execute-one! tx sql)
        ;
        ;;p (println "\nquery2" query2)
        ]

    )
  )



(spy (defn requester?
  [tx auth-entity]

  (let [
        query (-> (sql/select [[:exists

                                (-> (sql/select [true :exists])
                                    (sql/from :procurement_requesters_organizations)
                                    (sql/where [:= :procurement_requesters_organizations.user_id [:cast (:user_id auth-entity) :uuid]]))
                                ]]
                              )
                  sql-format)

        p (println ">o> requester??" query)

        result (jdbc/execute-one! tx (spy query))
        p (println ">o> requester??" (spy result))
        ]

    (spy (:exists result)))))


(defn advanced?
  [tx auth-entity]
  (spy (->> [viewer? inspector? admin?]
            (map #(% tx auth-entity))
            (some true?))))

(defn get-permissions
  [{{:keys [tx-next authenticated-entity]} :request} args value]

  (when (not= (:user_id authenticated-entity) (:id value))
    (raise "Not allowed to query permissions for a user other then the authenticated one."))
  {:isAdmin (admin? tx-next authenticated-entity),
   :isRequester (requester? tx-next authenticated-entity),
   :isInspectorForCategories (categories-perms/inspected-categories tx-next value),
   :isViewerForCategories (categories-perms/viewed-categories tx-next value)})
