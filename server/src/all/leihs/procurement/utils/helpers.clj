(ns leihs.procurement.utils.helpers
  (:require [cheshire.core :refer [generate-string] :rename
             {generate-string to-json}]
            [clj-time.core :as clj-time]
            [clojure.set :refer [subset?]]
            [clojure.tools.logging :as log]))

(defn submap? [m1 m2] (subset? (set m1) (set m2)))

(defn error-as-graphql-object
  [code message]
  {:errors [{:message message,
             :extensions {:code code,
                          :timestamp (-> (clj-time/now)
                                         .toString)}}],
   :data []})

(defn error-as-graphql
  [code message]
  (to-json (error-as-graphql-object code message)))
