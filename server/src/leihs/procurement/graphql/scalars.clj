(ns leihs.procurement.graphql.scalars
  (:require
    [java-time]
    [taoensso.timbre :refer [debug error info spy warn]])
  (:import (java.util UUID)))

(defn int-parse [x]
  (try
    (if (number? x) x (Integer/parseInt x))
    (catch Throwable _
      nil)))

(def scalars
  {:uuid-parse #(UUID/fromString %)
   :uuid-serialize str
   :int-parse int-parse
   :int-serialize identity})
