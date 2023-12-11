(ns leihs.procurement.resources.users
  (:require
    ;[ clojure.java.jdbc :as jdbc]
    [leihs.procurement.utils.sql :as sqlo]

    [taoensso.timbre :refer [debug info warn error spy]]


    [honey.sql :refer [format] :rename {format sql-format}]
    [leihs.core.db :as db]
    [next.jdbc :as jdbc]
    [honey.sql.helpers :as sql]

    [clojure.string :as clj-str]
    ))

(defn sql-order-users
  [sqlmap]
  (println ">oo >tocheck users3")
  (sql/order-by
    sqlmap
    (concat :users.firstname :users.lastname :users.login :users.id)))

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

      (-> (cond-> users-base-query is-requester
                  (sql/join :procurement_requesters_organizations
                            [:= :procurement_requesters_organizations.user_id :users.id])
                  (spy term-parts)

                  (sql/where (into [:and]

                                   (search-query search-term term-parts)

                                   ;(map
                                   ;  (fn [term-percent]
                                   ;    [:ilike (->> (concat
                                   ;                   :users.firstname
                                   ;                   [:cast " " :varchar]
                                   ;                   :users.lastname)
                                   ;                 [:unaccent])
                                   ;     [:unaccent term-percent]])
                                   ;  term-parts)

                                   ))

                  exclude-ids
                  (sql/where [:not-in :users.id exclude-ids]) offset
                  (sql/offset offset) limit
                  (sql/limit limit))
          sql-format
          spy))))                                           ;; TODO: SEARCH

















(comment

  (let [
        p (println "\nquery")


        search-term "Procurement Admin"
        term-parts "%Procurement% %Admin%"
        is-requester true
        exclude-ids nil
        offset 0
        limit 25

        ;test (fn [term-percent]
        ;  ["~~*"
        ;   (->> (sqlo/call :concat
        ;                  :users.firstname
        ;                  (sqlo/call :cast " " :varchar)
        ;                  :users.lastname)
        ;        (sqlo/call :unaccent))
        ;   (sqlo/call :unaccent term-percent)])

        ;formatted-name (sqlo/call :concat
        ;                       :users.firstname
        ;                       (sqlo/call :cast " " :varchar)
        ;                       :users.lastname)
        ;p (println "\n 1:" (sqlo/format formatted-name))
        ;
        ;unaccented-name (sqlo/call :unaccent formatted-name)
        ;p (println "\n 2:" (sqlo/format unaccented-name))
        ;
        ;
        ;unaccented-term (sqlo/call :unaccent term-parts)
        ;p (println "\n 3:" (sqlo/format unaccented-term))
        ;
        ;result ["~~*" unaccented-name unaccented-term]
        ;
        ;p (println "\nquery" (sqlo/format result))






        tx (db/get-ds-next)




        formatted-name [:concat
                        :users.firstname
                        [[:cast " " :varchar]]
                        :users.lastname]
        p (println "\n 1:" (sql-format formatted-name))


        unaccented-name [:unaccent formatted-name]
        p (println "\n 2:" (sql-format unaccented-name))


        unaccented-term [:unaccent term-parts]
        p (println "\n 3:" (sql-format unaccented-term))

        result [:or [:ilike [unaccented-name] [:ilike [unaccented-term]]]]
        p (println ">o> ???" result)


        query (-> (sql/select :*)
                  (sql/from :users)
                  (sql/where result)
                  )


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