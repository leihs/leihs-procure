(ns leihs.procurement.utils.helpers
  (:require [cheshire.core :refer [generate-string] :rename
             {generate-string to-json}]
            [clj-time.core :as time]
            [clojure.set :refer [subset?]]))

(defn submap? [m1 m2] (subset? (set m1) (set m2)))

(defn error-as-graphql
  [code message]
  {:errors [{:message message,
             :extensions {:code code, :timestamp (time/now)}}],
   :data []})
