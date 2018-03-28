(ns user
  (:require [leihs.procurement.schema :as s]
	    [leihs.procurement.utils.ds :as ds]
            [leihs.procurement.backend.run :as run]
	    [com.walmartlabs.lacinia :as lacinia]
            [clojure.tools.cli :as cli :refer [parse-opts]]
	    [clojure.java.browse :refer [browse-url]]
            [clojure.tools.logging :as logging]
	    [clojure.walk :as walk])
  (:import (clojure.lang IPersistentMap)))

(def schema (s/load-schema))

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

(defn start []
  (let [{:keys [options]} (cli/parse-opts () run/cli-options :in-order true)
        ds (ds/init {:database-url options})]
    (logging/info ds)
    ds))

; (q "{ request_by_id(id: \"91805c8c-0f47-45f1-bcce-b11da5427294\") { id article_name }}")
