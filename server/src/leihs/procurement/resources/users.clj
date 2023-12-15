(ns leihs.procurement.resources.users
  (:require
    ;[ clojure.java.jdbc :as jdbc]
    [leihs.procurement.utils.sql :as sqlo]

    [taoensso.timbre :refer [debug info warn error spy]]


    [leihs.procurement.utils.helpers :refer [add-comment-to-sql-format cast-uuids]]

    [honey.sql :refer [format] :rename {format sql-format}]
    [leihs.core.db :as db]
    [next.jdbc :as jdbc]
    [honey.sql.helpers :as sql]

    [clojure.string :as clj-str]
    ))

(defn sql-order-users
  [sqlmap]
  (println ">oo >tocheck users3 sql-order-users, sqlmap=" sqlmap)
  (sql/order-by sqlmap [[:concat :users.firstname :users.lastname :users.login :users.id]])
  )



(comment
  (let [
        tx (db/get-ds-next)
        request {:route-params {:user-id #uuid "c0777d74-668b-5e01-abb5-f8277baa0ea8"}
                 :tx-next tx}

        ;query (-> (sql/select :*)
        query (-> (sql/select :users.firstname :users.lastname :users.login :users.id)
                  (sql/from :users)
                  )

        query (sql-order-users query)

        result (jdbc/execute! tx (-> query
                                     sql-format
                                     ))

        p (println "\nquery" result)

        ]

    )
  )








(def users-base-query
  (-> (sql/select :users.id :users.firstname :users.lastname) ;; FIXME TODO
      (sql/from :users)
      sql-order-users))






(defn search-query [term-parts term-percent]


  (let [

        formatted-name [:concat
                        :users.firstname
                        [[:cast term-percent :varchar]]
                        :users.lastname]
        ;p (println "\n 1:" (sql-format formatted-name))


        unaccented-name [:unaccent formatted-name]
        ;p (println "\n 2:" (sql-format unaccented-name))


        unaccented-term [:unaccent term-parts]
        ;p (println "\n 3:" (sql-format unaccented-term))

        result [:or [:ilike [unaccented-name] [:ilike [unaccented-term]]]]
        ] result)

  )




;
;(defn cast-uuids [uuids]
;  (map (fn [uuid-str] [:cast uuid-str :uuid]) uuids))



(defn get-users
  [context args _]
  (println ">oo >tocheck users1")
  (jdbc/execute!
    (-> context
        :request
        :tx-next)
    (let [search-term (:search_term args)
          term-parts (and search-term
                          (map (fn [part] (str "%" part "%"))
                               (clj-str/split search-term #"\s+")))
          is-requester (:isRequester args)

          exclude-ids (:exclude_ids args)
          ;exclude-ids (cast-uuids (:exclude_ids args))

          offset (:offset args)
          limit (:limit args)

          ]

      (println ">o> >tocheck> search-term=" search-term)
      (println ">o> >tocheck> term-parts=" term-parts)
      (println ">o> >tocheck> is-requester=" is-requester)
      (println ">o> >tocheck> exclude-ids=" exclude-ids)
      (println ">o> >tocheck> offset=" offset)
      (println ">o> >tocheck> limit=" limit)


      ;>o> >tocheck> search-term= Procurement Admin
      ;>o> >tocheck> term-parts= (%Procurement% %Admin%)
      ;>o> >tocheck> is-requester= true
      ;>o> >tocheck> exclude-ids= nil
      ;>o> >tocheck> offset= 0
      ;>o> >tocheck> limit= 25
      ;>o> >tocheck> merge-where1= nil 0
      ;>o> >tocheck> merge-where2= (%Procurement% %Admin%)


      (-> (cond-> users-base-query
                  is-requester (sql/join :procurement_requesters_organizations
                                         [:=
                                          :procurement_requesters_organizations.user_id
                                          :users.id])
                  term-parts (sql/where (into [:and] (map (fn [x] [:ilike [:unaccent [:concat :users.firstname [:cast " " :varchar] :users.lastname]] [:unaccent [:cast x :varchar]]]) term-parts)))

                  ;term-parts (sql/merge-where
                  ;             (into [:and]
                  ;                   (map
                  ;                     (fn [term-percent]
                  ;                       ["~~*"
                  ;                        (->> (sql/call :concat
                  ;                                       :users.firstname
                  ;                                       (sql/call :cast " " :varchar)
                  ;                                       :users.lastname)
                  ;                             (sql/call :unaccent))
                  ;                        (sql/call :unaccent term-percent)])
                  ;                     term-parts))
                  ;
                  ;             )
                  exclude-ids (sql/where [:not-in :users.id (cast-uuids (spy exclude-ids))])
                  offset (sql/offset offset)
                  limit (sql/limit limit))
          sql-format
          spy))))                                           ;; TODO: SEARCH












(def list-if-nil (fn [x] (if (nil? x) '[] x)))





(comment

  (let [
        p (println "\nquery")

        search-term "Procurement Admin"
        term-parts "%Procurement% %Admin%"                  ;; iteration
        is-requester true
        exclude-ids (list-if-nil nil)
        offset 0
        limit 25

        tx (db/get-ds-next)


        ;; MASTER
        ;SELECT users.id, users.firstname, users.lastname
        ;FROM users
        ;INNER JOIN procurement_requesters_organizations
        ;ON procurement_requesters_organizations.user_id = users.id
        ;WHERE (unaccent(concat(users.firstname, CAST(' ' AS varchar), users.lastname)) ~~* unaccent('%Procurement%')
        ;               AND unaccent(concat(users.firstname, CAST(' ' AS varchar), users.lastname)) ~~* unaccent('%Admin%'))

        ;ORDER BY concat(users.firstname, users.lastname, users.login, users.id)
        ;LIMIT 25 OFFSET 0


        ;(sql/format (cond-> users-base-query is-requester
        ;                    (sql/merge-join :procurement_requesters_organizations [:= :procurement_requesters_organizations.user_id :users.id]) term-parts
        ;                    (sql/merge-where (into [:and] (map (spy (fn [term-percent]
        ;                                                              ["~~*" (->> (sql/call :concat :users.firstname (sql/call :cast " " :varchar) :users.lastname) (sql/call :unaccent)) (sql/call :unaccent term-percent)])) term-parts))) exclude-ids
        ;                    (sql/merge-where [:not-in :users.id exclude-ids]) offset
        ;                    (sql/offset offset) limit
        ;                    (sql/limit limit)))

        ;=> ["SELECT users.id, users.firstname, users.lastname FROM users INNER JOIN procurement_requesters_organizations ON procurement_requesters_organizations.user_id = users.id
        ; WHERE (unaccent(concat(users.firstname, CAST(? AS varchar), users.lastname)) ~~* unaccent(?) AND unaccent(concat(users.firstname, CAST(? AS varchar), users.lastname)) ~~* unaccent(?))
        ; ORDER BY concat(users.firstname, users.lastname, users.login, users.id) LIMIT ? OFFSET ?" " " "%Procurement%" " " "%Admin%" 25 0]








        term-parts "%Procurement% %Admin%"                  ;; iteration

        term-parts (and search-term
                        (map (fn [part] (str "%" part "%"))
                             (clj-str/split search-term #"\s+")))

        p (println ">o> ?term-parts?" term-parts)




        ;; Works
        ;term-parts-query (map (fn [x] [[[:unaccent [:concat :users.firstname [[:cast " " :varchar]] :users.lastname] x]]]) term-parts)
        term-parts-query (map (fn [x] [:ilike [:unaccent [:concat :users.firstname [:cast " " :varchar] :users.lastname]] [:unaccent [:cast x :varchar]]]) term-parts)
        p (println ">o> ???1a" (first term-parts-query))
        p (println ">o> ???1b" (second term-parts-query))
        where-clause (into [:and] term-parts-query)


        where-clause (into [:and] (map (fn [x] [:ilike [:unaccent [:concat :users.firstname [:cast " " :varchar] :users.lastname]] [:unaccent [:cast x :varchar]]]) term-parts))


        ;where-clause [:and [:ilike [[:unaccent [:concat :users.firstname [:cast " " :varchar] :users.lastname]]] ;; works
        ;                    [[:unaccent [:cast "%abcd%" :varchar]]]]]
        ;
        ;
        ;;; works
        ;where-clause [:and [:ilike [:unaccent [:concat :users.firstname [:cast " " :varchar] :users.lastname]] [:unaccent [:cast "%abcd%" :varchar]]]    ]





        ;exclude-ids (list-if-nil (#uuid  "39b75ad5-44d6-4ced-838c-a205a078baa5" #uuid "4c407438-36f9-4f2d-a737-2413356af468"))
        exclude-ids [#uuid "39b75ad5-44d6-4ced-838c-a205a078baa5" #uuid "4c407438-36f9-4f2d-a737-2413356af468"] ;;works
        exclude-ids (list-if-nil [])                        ;;ERROR: syntax error at or near ")"
        ;exclude-ids (list-if-nil nil)                      ;;ERROR: syntax error at or near ")"

        p (println ">o> ?exclude-ids?" exclude-ids)


        ;;where-clause [:unaccent term-parts]
        ;p (println "\n 3:" (sql-format where-clause))
        ;
        ;where-cond [:and [:ilike [unaccented-name] [:ilike [where-clause]]]]
        p (println ">o> ???2" where-clause)


        query (-> (sql/select :users.id :users.firstname :users.lastname)
                  (sql/from :users)
                  (sql/join :procurement_requesters_organizations [:= :procurement_requesters_organizations.user_id :users.id]) ;;? term-parts
                  (sql/where where-clause)                  ;;exclude-ids


                  ;(sql/where [:not-in :users.id (into [] exclude-ids)]) ;;offset
                  (sql/where [:not-in :users.id exclude-ids]) ;;offset
                  (sql/offset offset)                       ;;limit
                  (sql/limit limit))




        p (println "\nquery" (sql-format query))


        result (jdbc/execute! tx (sql-format query))

        p (println "\nquery" result)

        ]

    )
  )



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

(comment

  ; "SELECT users.id, users.firstname, users.lastname FROM users INNER JOIN procurement_requesters_organizations ON procurement_requesters_organizations.user_id = users.id


  ; WHERE (unaccent(concat(users.firstname, CAST(? AS varchar), users.lastname)) ~~* unaccent(?)
  ; AND unaccent(concat(users.firstname, CAST(? AS varchar), users.lastname)) ~~* unaccent(?))

  ; ORDER BY concat(users.firstname, users.lastname, users.login, users.id) LIMIT ? OFFSET ?" " " "%Procurement%" " " "%Admin%" 25 0]


  ;(sql/format (cond-> users-base-query is-requester
  ;                    (sql/merge-join :procurement_requesters_organizations [:= :procurement_requesters_organizations.user_id :users.id]) term-parts
  ;(sql/merge-where (into [:and] (map (spy (fn [term-percent]
  ;                                                              ["~~*" (->> (sql/call :concat :users.firstname
  ;                                                                                    (sql/call :cast " " :varchar) :users.lastname)
  ;                                                                          (sql/call :unaccent))
  ;                                                               (sql/call :unaccent term-percent)]))
  ;                                                       term-parts))) exclude-ids

  ;                    (sql/merge-where [:not-in :users.id exclude-ids]) offset
  ;                    (sql/offset offset) limit
  ;                    (sql/limit limit)))

  (let [
        tx (db/get-ds-next)
        request {:route-params {:user-id #uuid "c0777d74-668b-5e01-abb5-f8277baa0ea8"}
                 :tx tx}
        user-id #uuid "37bb3d3d-3a61-4f98-863e-c549568317f0"
        query (sql-format {:select :*
                           :from [:users]
                           :where [:= :id [:cast user-id :uuid]]})

        query2 (-> (sql/select :*)
                   (sql/from :users)
                   (sql/where [:= :id user-id])
                   sql-format
                   (->> (jdbc/execute! tx))
                   )

        p (println "\nquery" query)
        p (println "\nquery2" query2)
        ]

    )
  )