(ns leihs.admin.common.roles.core
  (:refer-clojure :exclude [str keyword])
  (:require
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.core.core :refer [keyword str presence]]
    #?@(:cljs
        [[reagent.ratom :as ratom :refer [reaction]]
         [leihs.core.routing.front :as routing]])))

(def hierarchy
  [:customer
   :group_manager
   :lending_manager
   :inventory_manager])

(def allowed-states
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

(defn set-roles [role newval]
  (case [role newval]
    [:customer false] (:none allowed-states)
    [:customer true] (:customer allowed-states)
    [:group_manager false] (:customer allowed-states)
    [:group_manager true] (:group_manager allowed-states)
    [:lending_manager false] (:group_manager allowed-states)
    [:lending_manager true] (:lending_manager allowed-states)
    [:inventory_manager false] (:lending_manager allowed-states)
    [:inventory_manager true] (:inventory_manager allowed-states)))

(defn aggregate [seq-of-roles]
  (reduce (fn [agg add]
            (->> hierarchy
                 (map (fn [role]
                        [role (or (role agg) (role add))]))
                 (into {})))
          (:none allowed-states)
          seq-of-roles))

(defn expand-to-hierarchy [role]
  (->> hierarchy
       reverse
       (drop-while #(not= (keyword role) %))
       reverse))

(defn expand-to-hierarchy-up-and-include [role]
  (->> hierarchy
       (drop-while #(not= (keyword role) %))))

(defn roles-to-map [roles]
  (->> hierarchy
       (map (fn [r] [r (.contains roles r)]))
       (into {})))

(def role-query-param*
  #?(:cljs
     (reaction
       (get-in @routing/state* [:query-params-raw :role] "customer"))))

(def filtered-by-role?*
  #?(:cljs
     (reaction
       (= "customer" @role-query-param*))))


(defn empty-alert []
  #?(:cljs
     [:div.alert.alert-warning.text-center
      [:p "This collection is empty for current role filter: "
       [:code @role-query-param*]]
      [:p [:a.btn.btn-outline-primary
           {:href (path (:path @routing/state*) {}
                        (assoc (:query-params-raw @routing/state*) :role ""))}
           "Show items indifferent of assigned role."]]]))


