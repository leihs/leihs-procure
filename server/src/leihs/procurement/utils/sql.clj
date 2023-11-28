(ns leihs.procurement.utils.sql
  (:refer-clojure :exclude [format update])
  (:require

    [clojure.java.jdbc :as jdbc]

    ;;; all needed imports
    ;      [honey.sql :refer [format] :rename {format sql-format}]
    (honeysql [format :as format] [helpers :as helpers]
              [types :as types] [util :refer [defalias]])
    [leihs.core.db :as db]
    [leihs.procurement.utils.sql :as sql]

    ;[next.jdbc :as jdbc]
    ;[honey.sql.helpers :as sql]


    [taoensso.timbre :refer [debug error info spy warn]]))

; regex
(defmethod format/fn-handler "~*"
  [_ field value]
  (str (format/to-sql field) " ~* " (format/to-sql value)))

; ilike
(defmethod format/fn-handler "~~*"
  [_ field value]
  (str (format/to-sql field) " ~~* " (format/to-sql value)))

(defn dedup-join
  [honeymap]
  (assoc honeymap
    :join (reduce #(let [[k v] %2] (conj %1 k v))
                  []
                  (clojure.core/distinct (partition 2 (:join honeymap))))))

(defn format
  "Calls honeysql.format/format with removed join duplications in sql-map."
  [sql-map & params-or-opts]
  (apply format/format [(dedup-join sql-map) params-or-opts]))

(defn map->where-clause
  ([m] (map->where-clause nil m))
  ([table m]
   "transforms {:foo 1, :bar 2} of table :baz into
   [:and [:= baz.foo 1] [:= :baz.bar 2]] or
   [:and [:in baz.foo [1 2]] [:= :baz.bar 3]]"
   (letfn [(add-table-name [k]
             (if table
               (-> table
                   name
                   (str "." (name k))
                   keyword)
               k))]
     (->> m
          (map (fn [[k v]]
                 (let [op (if (coll? v) :in :=)] [op (add-table-name k) v])))
          (cons :and)))))

(defn merge-where-false-if-empty
  [m c]
  (cond-> m (empty? c) (helpers/where [:= true false])))





(defn select-nest
  [sqlmap tbl nest-key]

  (println ">o> sqlmap" sqlmap)
  (println ">o> tbl" tbl)
  (println ">o> nest-key" nest-key)

  ;;adds:  [#sql/call [:row_to_json :users] :user])

  (spy (helpers/merge-select sqlmap [(types/call :row_to_json tbl) nest-key])))



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

  (require '[clojure.data.json :as json])

  (let [
        tx (db/get-ds)

        ;sql (-> (select :organization (sql/call :count :*))
        ;        (from :users)
        ;        (where [:and
        ;                [:= :organization "local"]
        ;                (raw "admin_protected = false")])
        ;        (group :organization)
        ;        )
        ;p (println ">o> sql1" (pr-str sql))


        ;sql (-> (select :id :lastname :organization)
        ;        (from :users)
        ;        (where [:and
        ;                [:= :organization "local"]
        ;                (raw "admin_protected = false")])
        ;        )
        ;p (println ">o> sql2" (pr-str sql))

        sql (-> (from :users))

        query (sql/select-nest sql :users :user)
        p (println "\n>o> nested1" query)
        ;>o> nested1 {:from (:users), :select ([#sql/call [:row_to_json :users] :user])}

        ;creates following format
        ;"user": {
        ;             "system_admin_protected": true,
        ;             "..
        ;             }

        p (println "\n>o> nested2a" (pr-str query))
        result (jdbc/query tx (format query))

        p (println "\nresult" (json/write-str result))      ;clojure-map to json
        ]

    )
  )

(comment

  (let [
        tx (db/get-ds)

        sql (-> (select :organization (sql/call :count :*))
                (from :users)
                (where [:and
                        [:= :organization "local"]
                        (raw "admin_protected = false")])
                (group :organization)
                )
        p (println ">o> sql1" (pr-str sql))


        sql (-> (select :id :lastname :organization)
                (from :users)
                (where [:and
                        [:= :organization "local"]
                        (raw "admin_protected = false")])
                )
        p (println ">o> sql2" (pr-str sql))


        ;p (println ">o> sql2" sql)
        sql-str "{:select (:organization #sql/call [:count :*]), :from (:users), :where [:and [:= :organization local] #sql/raw admin_protected = false], :group-by (:organization)}" ; original but broken console-output


        ;p (println ">o> sql2" (pr-str sql))
        sql-str "{:select (:id :lastname :organization), :from (:users), :where [:and [:= :organization \"local\"] #sql/raw \"admin_protected = false\"]}" ; original but broken console-output
        sql-str "{:select [:id :lastname :organization], :from [:users], :where [:and [:= :organization \"local\"] #sql/raw \"admin_protected = false\"]}" ; manually fixed


        ;sql-str "{:select [:organization #sql/call [:count :*]], :from [:users], :where [:and [:= :organization \"local\"] #sql/raw \"admin_protected = false\"], :group-by [:organization]}" ; manually fixed

        ;sql-str "{:select [:organization #sql/call [:count :*]] :from [:users] :where [:and [:= :organization \"local\"] #sql/raw \"admin_protected = false\"] :group-by [:organization]}" ; ok
        ;sql-str "{:select [:organization] :from [:users] :where [:and [:= :organization \"local\"] #sql/raw \"admin_protected = false\"]}" ; ok
        ;sql-str "{:select [:organization]  :from [:users]}" ; ok

        p (println ">o> sql3" sql-str)

        sql (eval (read-string sql-str))

        query (sql/format sql)
        result (jdbc/query tx query)

        p (println "\nquery" query)
        p (println "\nresult" result)
        ]

    )
  )

(defn join-and-nest
  ([sqlmap tbl join-cond nest-key]
   (join-and-nest sqlmap tbl join-cond nest-key helpers/merge-left-join))
  ([sqlmap tbl join-cond nest-key join-fn]
   (-> sqlmap
       (select-nest tbl nest-key)
       (join-fn tbl join-cond))))

(defalias call types/call)
(defalias param types/param)
(defalias raw types/raw)

(defalias format-predicate format/format-predicate)
(defalias quote-identifier format/quote-identifier)

(defalias delete-from helpers/delete-from)
(defalias from helpers/from)
(defalias group helpers/group)
(defalias insert-into helpers/insert-into)
(defalias join helpers/join)
(defalias limit helpers/limit)
(defalias merge-left-join helpers/merge-left-join)
(defalias merge-join helpers/merge-join)
(defalias merge-select helpers/merge-select)
(defalias merge-where helpers/merge-where)
(defalias modifiers helpers/modifiers)
(defalias offset helpers/offset)
(defalias order-by helpers/order-by)
;(defalias returning helpers/returning)
(defalias select helpers/select)
(defalias sset helpers/sset)
;(defalias using helpers/using)
(defalias update helpers/update)
(defalias values helpers/values)
(defalias where helpers/where)
