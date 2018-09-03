(ns leihs.procurement.utils.sql
  (:refer-clojure :exclude [format update])
  (:require [honeysql [format :as format] [helpers :as helpers]
             [types :as types] [util :as util :refer [defalias]]]))

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

;#### debug ###################################################################
;(logging-config/set-logger! :level :debug)
;(logging-config/set-logger! :level :info)
;(debug/debug-ns *ns*)
