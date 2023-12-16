(ns leihs.procurement.resources.categories
  (:require
    [taoensso.timbre :refer [debug info warn error spy]]

    ;[clojure.java.jdbc :as jdbc]
    ;        [leihs.procurement.utils.sql :as sql]


    [leihs.procurement.utils.helpers :refer [add-comment-to-sql-format cast-uuids]]

    [clojure.tools.logging :as log]
    [leihs.procurement.authorization :as authorization]
    [leihs.procurement.permissions.user :as user-perms]
    [leihs.procurement.resources [category :as category]
     [inspectors :as inspectors] [viewers :as viewers]]

    [honey.sql :refer [format] :rename {format sql-format}]
    [leihs.core.db :as db]
    [next.jdbc :as jdbc]
    [honey.sql.helpers :as sql]
    ))

(def categories-base-query                                  ;; broken query
  (-> (sql/select :procurement_categories.*)
      (sql/from :procurement_categories)
      (sql/order-by [:procurement_categories.name :asc])))


;(defn cast-uuids [uuids]
;  (map (fn [uuid-str] [:cast uuid-str :uuid]) uuids))

(defn categories-query
  [context arguments value]

  (println ">oo> tocheck value" value)
  (println ">oo> tocheck value" value)
  (let [id (:id arguments)
        p (println ">o> tocheck (not nil)" id)
        inspected-by-auth-user (:inspected_by_auth_user arguments)
        main-category-id (:id value)]

    ;(assert (and arguments (seq arguments))

    (sql-format (cond-> categories-base-query
                        id (sql/where [:in :procurement_categories.id (cast-uuids id)]) ;;TODO: BROKEN
                        main-category-id (sql/where
                                           [:= :procurement_categories.main_category_id
                                            main-category-id])
                        inspected-by-auth-user
                        (-> (sql/join :procurement_category_inspectors
                                      [:= :procurement_category_inspectors.category_id
                                       :procurement_categories.id])
                            (sql/where [:= :procurement_category_inspectors.user_id
                                        (-> context
                                            :request
                                            :authenticated-entity
                                            :user_id)])))
                )))

;(defn cast-uuids "DEPRS: can't handle duplicates" [uuids]
;  (spy (map (fn [uuid-str] [:cast uuid-str :uuid]) uuids)))

;(defn cast-uuids [uuids]
;  (let [
;        p (println ">o> uuids-sql" (class uuids))
;        uuids-sql (map (fn [uuid-str] [:cast uuid-str :uuid]) (set uuids))
;        p (println ">o> uuids-sql" uuids-sql)
;        ]
;    (spy uuids-sql)
;    )
;  ;(spy (map (fn [uuid-str] [:cast uuid-str :uuid]) (set uuids)))
;  )


(defn get-categories-for-ids
  [tx ids]

  (println ">o> ids1" ids)
  (println ">o> ids2" (class ids))
  (println ">o> ids3" (cast-uuids ids))
  (println ">>>id 4 ???????")

  (jdbc/execute! tx (add-comment-to-sql-format (-> categories-base-query
                                                   ;(sql/where [:in :procurement_categories.id ids])
                                                   (sql/where [:in :procurement_categories.id (cast-uuids ids)])
                                                   sql-format
                                                   spy) "categories/get-categories-for-ids"))
  )



(comment
  (let [
        tx (db/get-ds-next)

        ids [#uuid "6e02fbcc-c575-43dc-acac-5923fc070b0e" #uuid "6e02fbcc-c575-43dc-acac-5923fc070b2e" #uuid "6e02fbcc-c575-43dc-acac-5923fc070b0e"]
        ids ["6e02fbcc-c575-43dc-acac-5923fc070b0e" "6e02fbcc-c575-43dc-acac-5923fc070b2e" "6e02fbcc-c575-43dc-acac-5923fc070b0e"]

        test (add-comment-to-sql-format (-> categories-base-query
                                            (sql/where [:in :procurement_categories.id (cast-uuids ids)])
                                            sql-format
                                            spy) "categories/get-categories-for-ids")

        p (println ">o> " test)
        ]
    (jdbc/execute! tx test)
    )
  )



(defn get-for-main-category-id
  [tx main-cat-id]

  (println ">>>id 3 ???????")
  (println ">>>id 3 ???????  tocheck main-cat-id=" main-cat-id)

  (jdbc/execute! tx (spy (add-comment-to-sql-format (-> categories-base-query
                                                        (sql/where [:= :procurement_categories.main_category_id [:cast main-cat-id :uuid]])
                                                        sql-format
                                                        spy) "categories/get-categories-for-id"))
                 ))


;(defn add-comment-to-sql-format "helper for debugging sql"
;  ([sql-formatted]
;   (let [
;         first-element (str (first sql-formatted) (str " /* comment */"))
;         ]
;     (cons first-element (rest sql-formatted))))
;
;  ([sql-format comment]
;   (let [
;         first-element (str (first sql-format) (str " /*" comment "*/"))
;         ]
;     (cons first-element (rest sql-format))))
;  )
;
;(comment
;  (let [
;        tx (db/get-ds-next)
;
;        ;; examples to trigger errors
;        user-id "e7ac5011-fd0e-4838-9a0f-b7da5783eede"
;        ;user-id nil
;        user-id ""
;
;        x (-> (sql/select :* [true :debug-comment1])
;              (sql/from :users)
;              ;(sql/where [:= :id [:cast user-id :uuid]] [:= :firstname "Procurement"])
;              (sql/where [:= :id [:cast user-id :uuid]])
;              sql-format
;              )
;
;        p (println ">o> abc>>>>aa" x)
;        ;p (println ">o> abc>>>>" (jdbc/execute-one! tx x))
;
;        ;x (conj x "/*now-comment*/")
;
;        p (println "\n")
;
;        ;x (add-comment-to-sql-format x)
;        x (add-comment-to-sql-format x "servus du")
;
;        p (println ">o> abc>>>>a" x)
;        p (println ">o> abc>>>>b" (jdbc/execute-one! tx (spy x)))
;
;        ]
;    )
;  )

(defn get-categories
  [context arguments value]
  (if (= (:id arguments) [])
    []
    (->> (categories-query context arguments value)
         spy
         (jdbc/execute! (-> context
                            :request
                            :tx-next)))))

(defn delete-categories-for-main-category-id-and-not-in-ids!
  [tx mc-id ids]
  (jdbc/execute!
    tx
    (-> (sql/delete-from :procurement_categories)
        (sql/where [:= :procurement_categories.main_category_id [:cast mc-id :uuid]])
        (cond-> (not (empty? ids)) (sql/where [:not-in :procurement_categories.id (cast-uuids ids)])) ;;FIXME TODO ids
        sql-format
        spy
        )))

(defn update-categories!
  [tx mc-id cs]
  (loop [[c & rest-cs] cs
         c-ids []]
    (if c
      (do (if (:id c)
            (category/update-category! tx (dissoc c :inspectors :viewers))
            (category/insert-category! tx (dissoc c :id :inspectors :viewers)))
          (let [c-id (or (:id c)
                         (as-> c <>
                           (select-keys <> [:name :main_category_id])
                           (category/get-category tx <>)
                           (:id <>)))]
            (inspectors/update-inspectors! tx c-id (:inspectors c))
            (viewers/update-viewers! tx c-id (:viewers c))
            (recur rest-cs (conj c-ids c-id))))
      (delete-categories-for-main-category-id-and-not-in-ids! tx mc-id c-ids))))

(defn update-categories-viewers!
  [context args value]
  (spy (let [request (:request context)
        tx (:tx-next request)
        auth-user (:authenticated-entity request)
        categories (:input_data args)]
    (loop [[c & rest-cs] (spy categories)]
      (if-let [c-id (:id c)]
        (do (spy (authorization/authorize-and-apply
              #(spy (viewers/update-viewers! tx c-id (spy (:viewers c))))
              :if-any
              [#(spy (user-perms/admin? tx auth-user))
               #(spy (user-perms/inspector? tx auth-user c-id))
               ]))
            (recur rest-cs))
        (spy (jdbc/execute! tx (add-comment-to-sql-format (-> categories-base-query
                                                         (sql/where [:in :procurement_categories.id (cast-uuids (map :id categories))])
                                                         sql-format) "categories/update-categories-viewers!")))
        )))))


;master
;2023-12-16T16:23:13.056Z NX-41294 DEBUG [leihs.procurement.resources.categories:110] - (user-perms/admin? tx auth-user) => false
;2023-12-16T16:23:13.057Z NX-41294 DEBUG [leihs.procurement.resources.categories:111] - (user-perms/inspector? tx auth-user c-id) => true
;2023-12-16T16:23:13.061Z NX-41294 DEBUG [leihs.procurement.resources.viewers:40] - (delete-viewers-for-category-id! tx c-id) => (1)
;2023-12-16T16:23:13.063Z NX-41294 DEBUG [leihs.procurement.resources.viewers:41] - (not (empty? u-ids)) => true
;2023-12-16T16:23:13.087Z NX-41294 DEBUG [leihs.procurement.resources.viewers:33] - (jdbc/execute! tx (-> (sql/insert-into :procurement_category_viewers) (sql/values row-maps) sql/format)) => (3)
;2023-12-16T16:23:13.087Z NX-41294 DEBUG [leihs.procurement.resources.viewers:42] - (insert-viewers! tx (map (fn* [p1__43117#] (hash-map :user_id p1__43117# :category_id c-id)) u-ids)) => (3)
;2023-12-16T16:23:13.088Z NX-41294 DEBUG [leihs.procurement.resources.categories:107] - (viewers/update-viewers! tx c-id (:viewers c)) => (3)
;2023-12-16T16:23:13.089Z NX-41294 DEBUG [leihs.procurement.resources.categories:110] - (user-perms/admin? tx auth-user) => false
;2023-12-16T16:23:13.090Z NX-41294 DEBUG [leihs.procurement.resources.categories:111] - (user-perms/inspector? tx auth-user c-id) => false