(ns leihs.admin.sidebar
  (:require
   ["/admin-ui" :as UI]
   ["@fortawesome/free-solid-svg-icons" :as solids]
   [clojure.string]
   [leihs.admin.resources.inventory-pools.authorization :as pool-auth]
   [leihs.core.auth.core :as auth]
   [leihs.core.routing.front :as routing]))

(def data [{:section-title "Manage"
            :items [{:icon solids/faWarehouse #_solids/faLayerGroup
                     :href "/admin/inventory-pools/"
                     :children "Inventory Pools"
                     :authorizers [auth/admin-scopes?
                                   pool-auth/some-lending-manager?]}]}
           {:section-title "Administration"
            :items [{:icon solids/faUser
                     :href "/admin/users/"
                     :children "Users"
                     :authorizers [auth/admin-scopes?
                                   pool-auth/some-lending-manager?]}
                    {:icon solids/faUsers
                     :href "/admin/groups/"
                     :children "Groups"
                     :authorizers [auth/admin-scopes?
                                   pool-auth/some-lending-manager?]}]}
           {:section-title "Reports"
            :items [{:icon solids/faChartColumn
                     :href "/admin/statistics/"
                     :children "Statistics"
                     :authorizers [auth/admin-scopes?]}
                    {:icon solids/faFileExport
                     :href "/admin/inventory/"
                     :children "Inventory-Export"
                     :authorizers [auth/admin-scopes?]}
                    {:group-title "Audits"
                     :icon solids/faClipboardCheck
                     :items [{:icon solids/faArrowRightArrowLeft
                              :href "/admin/audited/changes/"
                              :children "Changes"
                              :authorizers [auth/system-admin-scopes?]}
                             {:icon solids/faCodePullRequest
                              :href "/admin/audited/requests/"
                              :children "Requests"
                              :authorizers [auth/system-admin-scopes?]}]}]}
                                        ;; {:icon solids/faHistory
                                        ;;  :href "/admin/audits/"
                                        ;;  :children "Legacy"
                                        ;;  :authorizers [auth/admin-scopes?]}]}]}
           {:section-title "Configuration"
            :items [{:icon solids/faTableList
                     :href "/admin/inventory-fields/"
                     :children "Fields"
                     :authorizers [auth/admin-scopes?]}
                    {:icon solids/faBuilding
                     :href "/admin/buildings/"
                     :children "Buildings"
                     :authorizers [auth/admin-scopes?]}
                    {:icon solids/faPersonShelter
                     :href "/admin/rooms/"
                     :children "Rooms"
                     :authorizers [auth/admin-scopes?]}
                    {:icon solids/faTruckField
                     :href "/admin/suppliers/"
                     :children "Suppliers"
                     :authorizers [auth/admin-scopes?]}
                    {:icon solids/faEnvelope
                     :href "/admin/mail-templates/"
                     :children "Mail Templates"
                     :authorizers [auth/admin-scopes?]}
                    {:group-title "Settings"
                     :icon solids/faCog
                     :items [{:icon solids/faLanguage
                              :href "/admin/settings/languages/"
                              :children "Languages"
                              :authorizers [auth/admin-scopes?]}
                             {:icon solids/faList
                              :href "/admin/settings/misc/"
                              :children "Miscellaneous"
                              :authorizers [auth/admin-scopes?]}
                             {:icon solids/faPaperPlane
                              :href "/admin/settings/smtp/"
                              :children "SMTP"
                              :authorizers [auth/system-admin-scopes?]}
                             {:icon solids/faShieldHalved
                              :href "/admin/settings/syssec/"
                              :children "System and Security"
                              :authorizers [auth/system-admin-scopes?]}
                             {:icon solids/faKey
                              :href "/admin/system/authentication-systems/"
                              :children "Authentication Systems"
                              :authorizers [auth/system-admin-scopes?]}]}]}])

(defn sidebar-section [& {:keys [title children]}]
  [:> UI/Components.Sidebar.Section {:title title} children])

(defn sidebar-group [& {:keys [title icon children]}]
  [:> UI/Components.Sidebar.Group {:title title :icon icon} children])

(defn sidebar-item [& {:keys [authorizers href icon children]
                       :or {authorizers [(constantly true)]}}]
  (when (auth/allowed? authorizers)
    [:> UI/Components.Sidebar.Item
     {:href href
      :icon icon
      :active (clojure.string/includes? (:path @routing/state*) href)}
     children]))

(defn process-items [items]
  (doall
   (for [item items]
     (if (:items item)
       [sidebar-group {:key (:group-title item)
                       :title (:group-title item)
                       :icon (:icon item)
                       :children (process-items (:items item))}]
       [sidebar-item (merge {:key (:href item)} item)]))))

(defn sidebar-items [data]
  (doall
   (for [element data]
     [sidebar-section {:key (:section-title element)
                       :title (:section-title element)
                       :children (process-items (:items element))}])))

(defn sidebar []
  [:> UI/Components.Sidebar
   (sidebar-items data)])
