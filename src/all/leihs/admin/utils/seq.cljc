(ns leihs.admin.utils.seq
  (:refer-clojure :exclude [str keyword])
  (:require
    [leihs.admin.utils.core :refer [keyword str presence]]))

(defn with-index [offset xs]
  (map-indexed (fn [idx x]
                 (assoc x :index (+ 1 offset idx)))
               xs))
