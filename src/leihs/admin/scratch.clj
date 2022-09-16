(ns leihs.admin.scratch
  (:refer-clojure :exclude [str keyword])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [next.jdbc.sql :as jdbc]
    [honey.sql :refer [format] :rename {format sql-format}]
    [honey.sql.helpers :as sql]
    [clojure.string :as str]
    [logbug.debug :as debug]
    [logbug.catcher :as catcher]
    [taoensso.timbre :refer [error warn info debug spy]]
    ))

(def pools [{:id "570cb7cc-3d8e-4cf1-b2b5-97664aca70e3" :name "A-Pool"}
            {:id "6e273271-dc20-4322-87a4-d9b52f69ce6f" :name "B-Pool"}
            {:id "c2fa9a26-82a6-4c4b-9759-00c134cf1778" :name "C-Pool"}
            {:id "c5766cc6-d84d-4d26-906e-5bd21290446f" :name "D-Pool"}
            {:id "67287cee-2a2d-497d-9009-e37037ed8ec9" :name "E-Pool"}])

(comment 
  (as-> pools <>
    (map #(do [(:id %) (:name %)]) <>)
    (into {} <>))
  
  
  )
