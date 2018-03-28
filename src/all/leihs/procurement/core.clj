(ns leihs.procurement.core
  (:require [leihs.procurement.schema :as s]
            [com.walmartlabs.lacinia :as lacinia]
            [clojure.walk :as walk])
  (:import (clojure.lang IPersistentMap)))

(def schema (s/load-schema))

(defn q [query-string]
  (simplify (lacinia/execute schema query-string nil nil)))

(defn simplify
  "Converts all ordered maps nested within the map into standard hash maps,
  and sequences into vectors, which makes for easier constants in the tests,
  and eliminates ordering problems."
  [m]
  (walk/postwalk
    (fn [node]
      (cond (instance? IPersistentMap node) (into {} node)
            (seq? node) (vec node)
            :else node))
    m))

(q "{ request_by_id(id: \"91805c8c-0f47-45f1-bcce-b11da5427294\") { id article_name }}")
