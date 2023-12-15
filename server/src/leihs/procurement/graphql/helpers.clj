(ns leihs.procurement.graphql.helpers
  (:require [cheshire.core :refer [generate-string] :rename
             {generate-string to-json}]
            [clj-time.core :as clj-time]
            [clojure.string :as string]
            [com.walmartlabs.lacinia [executor :as executor]]))

(defn error-as-graphql-object
  [code message]
  {:errors [{:message (str message), ; if message is nil convert to ""
             :extensions {:code code,
                          :timestamp (-> (clj-time/now)
                                         .toString)}}],
   :data []})

(defn error-as-graphql
  [code message]
  (to-json (error-as-graphql-object code message)))


