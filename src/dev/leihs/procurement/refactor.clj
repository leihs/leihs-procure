(ns leihs.procurement.refactor
  (:require [clojure.tools.logging :as log]))

(defmacro thread-first-macro
  [[f x & r]]
  (loop [x x
         result `((~f ~@r))]
    (if (seq? x)
      (let [[f2 x2 & r2] x] (recur x2 (cons (cons f2 r2) result)))
      (->> result
           (cons x)
           (cons '->)))))

(defn thread-first [form] (macroexpand-1 `(thread-first-macro ~form)))

(defn thread-last-h
  ([f-app] (thread-last-h f-app []))
  ([[f & rst] result]
   (let [[last-arg & rev-rst] (reverse rst)
         f-without-last (if (nil? rev-rst) f `(~f ~@(reverse rev-rst)))]
     (log/spy f-without-last)
     (if (seq? last-arg)
       (thread-last-h last-arg (cons f-without-last result))
       (cons last-arg (cons f-without-last result))))))

(defmacro thread-last-macro [f-app] (cons '->> (thread-last-h f-app)))

(defn thread-last [form] (macroexpand-1 `(thread-last-macro ~form)))
