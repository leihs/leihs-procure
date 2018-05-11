(ns leihs.procurement.refactor
  (:refer-clojure :exclude [->])
  (:require [clojure.tools.logging :as log]))

(defmacro ->macro [[f x & r]]
  (loop [x x result `((~f ~@r))]
    (if (seq? x)
      (let [[f2 x2 & r2] x]
        (recur x2 (cons (cons f2 r2) result)))
      (->> result
           (cons x)
           (cons '->)))))

(defn -> [form]
  (macroexpand-1 (seq ['->macro form])))
