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

(defn expand-to-hierarchy-up-and-include [role]
  (->> roles-hierarchy
       (drop-while #(not= (keyword role) %))))

(defn roles-to-map [roles]
  (->> roles-hierarchy
       (map (fn [r] [r (.contains roles r)]))
       (into {})))


#?(:cljs
   (defn roles-component
     [data {edit-mode? :edit-mode?
            on-change-handler :on-change-handler
            ks :ks
            :or {edit-mode? false
                 on-change-handler nil
                 ks [:roles]}}]
     [:div.mb-1
      (for [role roles-hierarchy]
        (let [enabled (get-in data (conj ks role) false)]
          (if (and (not enabled) (not edit-mode?))
            ^{:key role} [:div]
            ^{:key role} [:div.form-check
                          [:input.form-check-input
                           {:id role
                            :type :checkbox
                            :checked enabled
                            :on-change (fn [e] (on-change-handler role))
                            :disabled (not edit-mode?)
                            }]
                          [:label.form-check-label
                           {:for role}
                           [:span " " role]]])))]))
