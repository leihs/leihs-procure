(ns leihs.procurement.utils.helpers
  (:require [clojure.set :refer [subset?]]))

(defn submap? [m1 m2] (subset? (set m1) (set m2)))

(defn reject-keys [m ks] (reduce #(dissoc %1 %2) m ks))
