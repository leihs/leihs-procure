(ns leihs.admin.resources.inventory.paths
  (:refer-clojure :exclude [str keyword])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [bidi.verbose :refer [branch param leaf]]
    ))

(def paths
  (branch "/inventory/"
          (leaf "" :inventory)
          (leaf "csv" :inventory-csv)
          (leaf "quick_csv" :inventory-quick-csv)
          (leaf "excel" :inventory-excel)
          (leaf "quick_excel" :inventory-quick-excel)))
