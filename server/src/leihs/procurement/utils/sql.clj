(ns leihs.procurement.utils.sql
  (:refer-clojure :exclude [format update])
  (:require

    ;; all needed imports
    [clojure.data.json :as json]
    [honey.sql :refer [format] :rename {format sql-format}]
    [honey.sql.helpers :as sql]
    (honeysql [format :as format] [helpers :as helpers]
              [types :as types] [util :refer [defalias]])

    [leihs.core.db :as db]

    [next.jdbc :as jdbc]


    [taoensso.timbre :refer [debug error info spy warn]]))

; regex
(defmethod format/fn-handler "~*"
  [_ field value]
  (str (format/to-sql field) " ~* " (format/to-sql value)))

;; ilike
;(defmethod format/fn-handler "~~*"
;  [_ field value]
;  (str (format/to-sql field) " ~~* " (format/to-sql value)))

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

(defn map->where-clause                                     ;;TODO
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

  ;(helpers/merge-select sqlmap [(types/call :row_to_json tbl) nest-key]))
  ;(sql/select sqlmap [:call :row_to_json tbl] nest-key)    )) ;FIXME


  ;>o> nested1 {:from (:users), :select ([#sql/call [:row_to_json :users] :user])}

  (sql/select sqlmap [[[:row_to_json tbl]] nest-key])       ;works
  )



(comment

  (require '[clojure.data.json :as json])

  (let [
        tx (db/get-ds)

        i (require '[clojure.data.json :as json])

        sql (-> (from :users))
        ;query (select-nest sql :users :user)                ;works
        query (select-nest sql :users "user")               ;works
        ;>o> nested {:from (:users), :select [[[[:row_to_json :users]] "user"]]}


        ;; FYI: creates following format
        ;"user": {
        ;             "system_admin_protected": true,
        ;             "..
        ;             }


        ;; ============

        ;key-name "user"
        ;table-name :users
        ;
        ;;query (-> (sql/select [[[:row_to_json :users.*]] :abc])        ;works
        ;;query (-> (sql/select [[[:row_to_json :users]] :abc])          ;works
        ;query (-> (sql/select [[[:row_to_json table-name]] key-name])   ;works
        ;        (sql/from :users)
        ;        )

        ;; ============

        p (println "\n>o> nested" (pr-str query))
        result (jdbc/execute-one! tx (sql-format query))

        p (println "\nresult" (json/write-str result))      ;clojure-map to json
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
