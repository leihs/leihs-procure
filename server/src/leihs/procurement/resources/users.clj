(ns leihs.procurement.resources.users
  (:require [clojure.java.jdbc :as jdbc]

                [taoensso.timbre :refer [debug info warn error spy]]


            [clojure.string :as clj-str]
            [leihs.procurement.utils.sql :as sql]))

(defn sql-order-users
  [sqlmap]
  (sql/order-by
    sqlmap
    (sql/call :concat :users.firstname :users.lastname :users.login :users.id)))

(def users-base-query
  (-> (sql/select :users.id :users.firstname :users.lastname)
      (sql/from :users)
      sql-order-users))

(defn get-users
  [context args _]

  (println ">o> users::get-users" get-users)

  (jdbc/query
    (-> context
        :request
        :tx)
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


      (println ">o> >tocheck> merge-where1=" exclude-ids offset)
      (println ">o> >tocheck> merge-where2=" term-parts)

      (-> (cond-> users-base-query is-requester
                  (sql/merge-join :procurement_requesters_organizations
                                  [:=
                                   :procurement_requesters_organizations.user_id
                                   :users.id])
                    term-parts
                  (sql/merge-where
                    (into [:and]
                          (map

                            (spy (fn [term-percent]
                              ["~~*"
                               (->> (sql/call :concat
                                              :users.firstname
                                              (sql/call :cast " " :varchar)
                                              :users.lastname)
                                    (sql/call :unaccent))
                               (sql/call :unaccent term-percent)]))

                            term-parts)))
                    exclude-ids
                  (sql/merge-where [:not-in :users.id exclude-ids]) offset
                  (sql/offset offset) limit
                  (sql/limit limit))
          sql/format
          spy
          )

      )))
