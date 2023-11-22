(ns leihs.admin.resources.inventory-pools.inventory-pool.entitlement-groups.entitlement-group.main
  (:refer-clojure :exclude [str keyword])
  (:require
   [cljs.pprint :refer [pprint]]
   [leihs.admin.common.components.table :as table]
   [leihs.admin.resources.inventory-pools.inventory-pool.core :as inventory-pool]
   [leihs.admin.resources.inventory-pools.inventory-pool.entitlement-groups.entitlement-group.core :as entitlement-group :refer [header tabs]]
   [leihs.admin.resources.inventory-pools.inventory-pool.entitlement-groups.entitlement-group.users.main]
   [leihs.admin.state :as state]
   [leihs.core.core :refer [str]]
   [react-bootstrap :as BS :refer [Button]]))

;; (defn groups-count-component []
;;   [:span.entitlement-group-groups-count
;;    [routing/hidden-state-component
;;     {:did-mount entitlement-group/fetch}]
;;    (if-let [count (:groups_count @entitlement-group/data*)]
;;      [:span count " " (pluralize-noun count "Group")]
;;      [:span  "?"])])
;;
;; (defn users-count-component []
;;   [:span.entitlement-group-users-count
;;    [routing/hidden-state-component
;;     {:did-mount entitlement-group/fetch}]
;;    (if-let [count (:users_count @entitlement-group/data*)]
;;      [:span count " " (pluralize-noun count "User")]
;;      [:span  "?"])])

(defn debug-component []
  (when (:debug @state/global-state*)
    [:section.debug
     [:hr]
     [:h2 "Page Debug"]
     [:div
      [:h3 "@data*"]
      [:pre (with-out-str (pprint @entitlement-group/data*))]]]))

;; TODO: To be deleted
;; (defn breadcrumbs-component []
;;   [breadcrumbs/nav-component
;;    @breadcrumbs/left*
;;    [[:li.breadcrumb-item
;;      [:a.btn.btn-outline-primary.btn.btn-sm.pt-0.pb-0.pl-1.pr-1
;;       {:href (str "/manage/"
;;                   @inventory-pool/id* "/groups/"
;;                   @entitlement-group/id* "/edit")}
;;       [icons/edit] " Edit (in \"lending\")"]]
;;     [breadcrumbs/users-li]
;;     [breadcrumbs/groups-li]]])

(defn default-value-component [value]
  [:span (str value)])

(def table-conf
  [{:key :name
    :label "Name"}
   {:key :is_verification_required
    :label "Verification required"}
   {:key :entitlements_count
    :label "Number of models"}
   {:key :users_count
    :label "Number of users"}
   {:key :direct_users_count
    :label "Number of direct users"}
   {:key :groups_count
    :label "Number of groups"}])

;; (defn title-component []
;;   [:h1 "Entitlement-Group "
;;    [entitlement-group/name-link-component]
;;    " in the Inventory-Pool "
;;    [inventory-pool/name-link-component]])

(defn edit-group-button []
  [:> Button
   {:variant "primary"
    :href (str "/manage/"
               @inventory-pool/id* "/groups/"
               @entitlement-group/id* "/edit")}
   "Edit"])

(defn overview-table []
  [:section
   (when-let [data @entitlement-group/data*]
     [:div
      [table/container {:className "entitlement-group-overview-table"
                        :header [:tr
                                 [:th "Property"]
                                 [:th "Value"]]
                        :body (for [{key :key label :label value-component :value-component
                                     :or {value-component default-value-component}} table-conf]
                                [:tr {:key key}
                                 [:td.label label]
                                 [:td.value [value-component (-> data key)]]])}]])])
       ;; [:tr
       ;;  [:th "Property"]
       ;;  [:th "Value"]]
       ;; (for [{key :key label :label value-component :value-component
       ;;        :or {value-component default-value-component}} table-conf]
       ;;   [:tr {:key key}
       ;;    [:td.label label]
       ;;    [:td.value [value-component (-> data key)]]])

(defn page []
  [:article.entitlement-group
   [header]
   [tabs]
   (when (and @inventory-pool/id* @entitlement-group/id*)
     [:<>
      [overview-table]
      [edit-group-button]])
   [debug-component]])
