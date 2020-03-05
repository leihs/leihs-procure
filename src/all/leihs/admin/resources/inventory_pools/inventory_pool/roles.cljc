(ns leihs.admin.resources.inventory-pools.inventory-pool.roles
  (:refer-clojure :exclude [str keyword])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    ))

(def roles-hierarchy
  [:customer
   :group_manager
   :lending_manager
   :inventory_manager])

(def allowed-roles-states
  {:none {:customer false
          :group_manager false
          :lending_manager false
          :inventory_manager false}
   :customer {:customer true
              :group_manager false
              :lending_manager false
              :inventory_manager false}
   :group_manager {:customer true
                   :group_manager true
                   :lending_manager false
                   :inventory_manager false}
   :lending_manager {:customer true
                     :group_manager true
                     :lending_manager true
                     :inventory_manager false}
   :inventory_manager {:customer true
                       :group_manager true
                       :lending_manager true
                       :inventory_manager true}})

(defn expand-role-to-hierarchy [role]
  (->> roles-hierarchy
       reverse
       (drop-while #(not= (keyword role) %))
       reverse))

(defn roles-to-map [roles]
  (->> roles-hierarchy
       (map (fn [r] [r (.contains roles r)]))
       (into {})))
