(ns leihs.admin.resources.inventory-pools.inventory-pool.groups.main
  (:require
   [cljs.core.async :refer [<! go]]
   [leihs.admin.common.components.filter :as filter]
   [leihs.admin.common.components.table :as table]
   [leihs.admin.common.roles.components :refer [put-roles< roles-component]]
   [leihs.admin.common.roles.core :as roles]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.groups.main :as groups]
   [leihs.admin.resources.inventory-pools.inventory-pool.core :as inventory-pool]
   [leihs.admin.state :as state]
   [leihs.core.routing.front :as routing]))

;### roles ####################################################################

(defn roles-update-handler [roles group]
  (go (swap! groups/data* assoc-in
             [(:route @routing/state*) :groups (:page-index group) :roles]
             (<! (put-roles<
                  (path :inventory-pool-group-roles
                        {:inventory-pool-id @inventory-pool/id*
                         :group-id (:id group)})
                  roles)))))

(defn roles-th-component  []
  [:th.pl-5 {:key :roles} " Roles "])

(defn roles-td-component [group]
  [:td.pl-5 {:key :roles}
   [roles-component
    (get group :roles)
    :message (str (:count_users group))
    :compact true
    :update-handler #(roles-update-handler % group)

    :label "Role"
    :query-params-key :role
    :default-option "customer"]])

;### actions ##################################################################

(defn form-role-filter []
  [filter/select-component
   :label "Role"
   :query-params-key :role
   :default-option "customer"
   :options (merge {"" "(any role or none)"
                    "none" "none"}
                   (->> roles/hierarchy
                        (map (fn [%1] [%1 %1]))
                        (into {})))])

(defn filter-section []
  [filter/container
   [:<>
    [filter/form-term-filter-component :placeholder "Name of the Group"]
    [filter/form-including-user]
    [form-role-filter]
    [filter/form-per-page]
    [filter/reset]]])

;### main #####################################################################

(defn debug-component []
  (when (:debug @state/global-state*)
    [:div]))

(defn page []
  [:article.inventory-pool-groups
   [routing/hidden-state-component
    {:did-mount (fn [_] (inventory-pool/clean-and-fetch))}]
   [inventory-pool/header]
   [inventory-pool/tabs]
   [routing/hidden-state-component
    {:did-change groups/fetch-groups}]
   [filter-section]
   [table/toolbar]
   [groups/table-component
    [groups/name-th-component groups/users-count-th-component roles-th-component]
    [groups/name-td-component groups/users-count-td-component roles-td-component]]
   [table/toolbar]
   [debug-component]
   [groups/debug-component]])
