(ns leihs.admin.resources.inventory.paths
  (:require [bidi.verbose :refer [branch leaf]]))

(def paths
  (branch "/inventory/"
          (leaf "" :inventory)
          (leaf "csv" :inventory-csv)
          (leaf "quick_csv" :inventory-quick-csv)
          (leaf "excel" :inventory-excel)
          (leaf "quick_excel" :inventory-quick-excel)))
