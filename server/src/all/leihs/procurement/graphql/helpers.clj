(ns leihs.procurement.graphql.helpers
  (:require [cheshire.core :refer [generate-string] :rename
             {generate-string to-json}]
            [clj-time.core :as clj-time]
            [com.walmartlabs.lacinia [executor :as executor]]))

(defn add-resource-type [m t] (assoc m :resource-type t))

(defn add-to-parent-values
  [resolved-value parent-value]
  (if parent-value
    (let [parent-values (:parent-values parent-value)
          new-parent-values
            (if parent-values (conj parent-values parent-value) [parent-value])]
      (merge resolved-value {:parent-values new-parent-values}))
    resolved-value))

(defn get-categories-args-from-selections-tree
  [context]
  (some-> context
          executor/selections-tree
          :BudgetPeriod/main_categories :selections
          :MainCategory/categories :args))

(defn get-categories-args-from-context
  [context]
  (or (:categories-args context)
      (get-categories-args-from-selections-tree context)))

(defn get-requests-args-from-selections-tree
  [context]
  (some-> context
          executor/selections-tree
          :BudgetPeriod/main_categories :selections
          :MainCategory/categories :selections
          :Category/requests :args))

(defn get-requests-args-from-context
  [context]
  (or (:requests-args context)
      (get-requests-args-from-selections-tree context)))

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
