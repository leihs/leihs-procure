(ns leihs.procurement.utils.sql
  (:require
   [honey.sql.helpers :as sql]
   [taoensso.timbre :refer [debug error info spy warn]]))

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
  (cond-> m (empty? c)
          (-> (dissoc :where)
              (sql/where [:= true false]))))

(defn select-nest
  [sqlmap tbl nest-key]
  (sql/select sqlmap [[:row_to_json tbl] nest-key]))

(defn join-and-nest
  ([sqlmap tbl join-cond nest-key]
   (join-and-nest sqlmap tbl join-cond nest-key sql/left-join))
  ([sqlmap tbl join-cond nest-key join-fn]
   (-> sqlmap
       (select-nest tbl nest-key)
       (join-fn tbl join-cond))))
