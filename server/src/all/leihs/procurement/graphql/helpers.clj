(ns leihs.procurement.graphql.helpers)

(defn add-resource-type [m t] (assoc m :resource-type t))

(defn add-to-parent-values
  [resolved-value parent-value]
  (if parent-value
    (let [parent-values (:parent-values parent-value)
          new-parent-values
            (if parent-values (conj parent-values parent-value) [parent-value])]
      (merge resolved-value {:parent-values new-parent-values}))
    resolved-value))
