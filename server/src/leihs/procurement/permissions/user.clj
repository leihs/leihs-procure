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


;(ns leihs.my.back.html
;    (:refer-clojure :exclude [keyword str])
;    (:require
;      [hiccup.page :refer [html5]]
;      [honey.sql :refer [format] :rename {format sql-format}]
;      [honey.sql.helpers :as sql]
;      [leihs.core.http-cache-buster2 :as cache-buster]
;      [leihs.core.json :refer [to-json]]
;      [leihs.core.remote-navbar.shared :refer [navbar-props]]
;      [leihs.core.shared :refer [head]]
;      [leihs.core.url.core :as url]
;      [leihs.my.authorization :as auth]
;      [leihs.core.db :as db]
;      [next.jdbc :as jdbc]))


;(defn true? #(= % true))

(defn is-truthy? [b]
  (= true b)
  )


(comment

  (let [
        user-id #uuid "3eaba478-f710-4cb8-bc87-54921a27e3bb" ;; >>3 [{:has_entry true}]
        user-id #uuid "3eaba478-f710-4cb8-bc87-54921a27e3b2" ;; >>3 []
        auth-entity {:user-id user-id}

        tx (db/get-ds-next)

        ;sql   (-> (sql/select true)
        ;          (sql/from :procurement_admins)
        ;          (sql/where [:= :procurement_admins.user_id user-id])
        ;
        ;          sql-format
        ;          )
        ;
        ;
        ;p (println ">>2" sql)



        ;result   (-> (sql/select true)
        ;result   (-> (sql/select :*)
        ;          (sql/from :procurement_admins)
        ;          (sql/where [:= :user_id [:cast user-id :uuid]])
        ;
        ;          sql-format
        ;          (->> jdbc/execute-one! tx)
        ;          )










        ;;; TODO: WORKS
        ;result (-> (sql/select :*)
        ;           (sql/from :procurement_admins)
        ;           ;(sql/where [:= :user_id [:cast user-id :uuid]])
        ;
        ;           sql-format
        ;           (->> (jdbc/execute! tx)))
        ;p (println ">>3" result)










        ;;; TODO: WORKS
        ;sql {:select :* :from :procurement_admins :where [:= :user_id [:cast user-id :uuid]]}
        ;p (println ">>3" sql)
        ;query (sql-format sql)
        ;p (println ">>3" query)
        ;result (jdbc/execute-one! tx query)
        ;
        ;p (println ">>3" result)
        ;result (:user_id (jdbc/execute-one! tx query))
        ;
        ;p (println ">>3" result)







        ;result (-> (sql/select [true :has_entry])
        ;           (sql/from :procurement_admins)
        ;           (sql/where [:= :user_id [:cast user-id :uuid]])
        ;
        ;           sql-format
        ;           (->> (jdbc/execute-one! tx)))
        ;p (println ">>3" result)





        ;;; TODO: WORKS
        ;result (-> (sql/select :exists)
        ;           (sql/from :procurement_admins)
        ;           (sql/where [:= :user_id [:cast user-id :uuid]])
        ;
        ;           sql-format
        ;           (->> (jdbc/execute! tx)))



        ;https://github.com/seancorfield/honeysql/blob/f46dbc5ca7010443f310ab4a97cd2871768af11a/test/honey/bigquery_test.cljc#L46
        user-id #uuid "3eaba478-f710-4cb8-bc87-54921a27e3bb" ;; >>3 [{:has_entry true}]
        ;user-id #uuid "3eaba478-f710-4cb8-bc87-54921a27e3b2"                  ;; >>3 []
        auth-entity {:user_id user-id}

        ;result (->
        ;         (sql/select :*)
        ;           ;(sql/select [[:case-expr [:= :user_id [:cast user-id :uuid]] "true"] "false"])
        ;
        ;           (sql/select [[:case-expr [:= :user_id [:cast user-id :uuid]] "true"] "false"]) ;; rewrite
        ;
        ;         (sql/from :procurement_admins)
        ;
        ;                    sql-format
        ;                    )
        ;
        ;p (println ">>3" result)
        ;result (jdbc/execute! tx result)






        ;result (-> (sql/select [true :has_entry])
        ;           (sql/from :procurement_admins)
        ;           (sql/where [:= :user_id [:cast user-id :uuid]])
        ;
        ;           sql-format
        ;           (->> (jdbc/execute-one! tx))
        ;           (:has_entry)
        ;           (true?))
        ;
        ;;result (true? (:has_entry result))



        result (admin? tx auth-entity)

        p (println ">>3" result)




        p (println ">>3" result)


        ;^{:return-type Long


        ;result (jdbc/execute! tx ["select * from procurement_admins where user_id"])

        ;p (admin? tx auth-entity)

        ;p (println "\nquery" query)
        ;p (println "\nquery2" query2)
        ]

    result
    )
  )

(defn admin? "Returns boolean"
  [tx auth-entity]
  (-> (sql/select [true :has_entry])
      (sql/from :procurement_admins)
      (sql/where [:= :user_id [:cast (:user_id auth-entity) :uuid]])
      sql-format
      (->> (jdbc/execute-one! tx))
      (:has_entry)
      (is-truthy?))

  )


(comment

  ;[honey.sql :refer [format] :rename {format sql-format}]
  ;[leihs.core.db :as db]
  ;[next.jdbc :as jdbc]
  ;[honey.sql.helpers :as sql]

  (let [
        ;user-id #uuid "3eaba478-f710-4cb8-bc87-54921a27e3bb" ;; >>3 [{:has_entry true}]
        ;user-id #uuid "3eaba478-f710-4cb8-bc87-54921a27e3b2" ;; >>3 []
        user-id #uuid "3eaba478-f710-4cb8-bc87-54921a27e3bb" ;; >>3 []
        ;user-id nil                                         ;; >>3 []
        auth-entity {:user_id user-id}

        c-id nil
        ;c-id #uuid "1efc2279-bc42-490c-b004-dca03813a6ef"

        tx (db/get-ds-next)

        ]
    ;(inspector? tx auth-entity c-id)
    (viewer? tx auth-entity c-id)
    )

  )


(defn inspector?
  ([tx auth-entity] (inspector? tx auth-entity nil))
  ([tx auth-entity c-id]

   (let [
         has-entry (-> (sql/select [true :has_entry])
                       (sql/from :procurement_category_inspectors)
                       (sql/where
                         [:= :procurement_category_inspectors.user_id
                          (:user_id auth-entity)]))

         res (cond-> has-entry
                     c-id (sql/where [:= :category_id c-id]))

         query (sql-format res)

         p (println "\n>2>" query "\n")                     ;;TODO

         res (jdbc/execute-one! tx query)
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

         p (println "\n>2>" query "\n")                     ;;TODO

         res (jdbc/execute-one! tx query)
         ]
     (is-truthy? (:has_entry res))
     )

   ))












(defn requester?
  [tx auth-entity]

  (-> (sql/select [true :has_entry])
      (sql/from :procurement_requesters_organizations)
      (sql/where [:= :user_id [:cast (:user_id auth-entity) :uuid]])
      sql-format
      (->> (jdbc/execute-one! tx))
      (:has_entry)
      (is-truthy?))

  ;(:result
  ;  (first
  ;    (jdbc/execute!
  ;      tx
  ;      (-> (sql/select
  ;            [(
  ;               ;sql/call
  ;               :exists
  ;               (-> (sql/select true)
  ;                   (sql/from :procurement_requesters_organizations)
  ;                   (sql/where
  ;                     [:= :procurement_requesters_organizations.user_id
  ;                      (:user_id auth-entity)]))) :result])
  ;          sql-format))))



  )

(defn advanced?
  [tx auth-entity]
  (->> [viewer? inspector? admin?]
       (map #(% tx auth-entity))
       (some true?)))

(defn get-permissions
  [{{:keys [tx-next tx authenticated-entity]} :request} args value]
  (when (not= (:user_id authenticated-entity) (:id value))
    (raise "Not allowed to query permissions for a user other then the authenticated one."))
  (spy {:isAdmin (admin? tx-next authenticated-entity),
   :isRequester (requester? tx-next authenticated-entity),
   :isInspectorForCategories (categories-perms/inspected-categories tx-next value),
   :isViewerForCategories (categories-perms/viewed-categories tx-next value)}))
