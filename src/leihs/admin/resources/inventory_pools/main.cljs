(ns leihs.admin.resources.inventory-pools.main
  (:require
   [cljs.pprint :refer [pprint]]
   [leihs.admin.common.components.filter :as filter]
   [leihs.admin.common.components.table :as table]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.common.icons :as icons]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.inventory-pools.authorization :as pool-auth]
   [leihs.admin.resources.inventory-pools.inventory-pool.core :as pool-core]
   [leihs.admin.resources.inventory-pools.inventory-pool.create :as create]
   [leihs.admin.resources.inventory-pools.shared :as shared]
   [leihs.admin.state :as state]
   [leihs.admin.utils.misc :refer [wait-component fetch-route*]]
   [leihs.core.auth.core :as auth-core]
   [leihs.core.json :as json]
   [leihs.core.routing.front :as routing]
   [reagent.core :as reagent :refer [reaction]]))

(def current-query-parameters*
  (reaction (-> @routing/state* :query-params
                (assoc :term (-> @routing/state* :query-params-raw :term)
                       :order (some-> @routing/state* :query-params
                                      :order clj->js json/to-json)))))

(def current-query-parameters-normalized*
  (reaction (shared/normalized-query-parameters @current-query-parameters*)))

(def current-route*
  (reaction
   (path (:handler-key @routing/state*)
         (:route-params @routing/state*)
         @current-query-parameters-normalized*)))

(def data* (reagent/atom nil))

(defn fetch []
  (http-client/route-cached-fetch
   data* {:route @fetch-route*
          :reload true}))

;;; helpers ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn page-path-for-query-params [query-params]
  (path (:handler-key @routing/state*)
        (:route-params @routing/state*)
        (merge @current-query-parameters-normalized*
               query-params)))

;;; Filter ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn filter-section []
  [filter/container
   [:<>
    [filter/form-term-filter-component :placeholder "Name of the Inventory Pool"]
    [filter/select-component
     :label "Active"
     :query-params-key :active
     :options {"" "any value" "yes" "yes" "no" "no"}]
    [filter/form-per-page]
    [filter/reset]]])

;;; Table ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn table-head [& [more-cols]]
  [:tr
   [:th "Index"]
   [:th "Active"]
   [:th "Short name"]
   [:th "Name " [:a {:href (page-path-for-query-params
                            {:order (-> [[:name :asc] [:id :asc]]
                                        clj->js json/to-json)})} "↓"]]
   [:th "# Users " [:a {:href (page-path-for-query-params
                               {:order (-> [[:users_count :desc] [:id :asc]]
                                           clj->js json/to-json)})} "↓"]]
   [:th "# Delegations " [:a {:href (page-path-for-query-params
                                     {:order (-> [[:delegations_count :desc] [:id :asc]]
                                                 clj->js json/to-json)})} "↓"]]
   (for [col more-cols]
     col)])

(defn link-to-inventory-pool [inventory-pool inner]
  (let [id (:id inventory-pool)]
    (if (or
         (auth-core/current-user-admin-scopes?)
         (pool-auth/current-user-is-some-manager-of-pool? id))
      [:a {:href (path :inventory-pool {:inventory-pool-id id})}
       inner]
      [:span.text-info inner])))

(defn table-row [inventory-pool more-cols]
  [:tr.inventory-pool {:key (:id inventory-pool)}
   [:td (:index inventory-pool)]
   [:td (if (:is_active inventory-pool) "yes" "no")]
   [:td (:shortname inventory-pool)]
   [:td (link-to-inventory-pool inventory-pool [:span (:name inventory-pool)])]
   [:td.users_count (:users_count inventory-pool)]
   [:td.delegations_count (:delegations_count inventory-pool)]
   (for [col more-cols]
     (col inventory-pool))])

(defn table-body [inventory-pools tds]
  [:<>
   (doall (for [inventory-pool inventory-pools]
            (table-row inventory-pool tds)))])

(defn inventory-pools-table [& [hds tds]]
  (if-not (contains? @data* @fetch-route*)
    [wait-component]
    (if-let [inventory-pools (-> @data*
                                 (get @fetch-route*)
                                 :inventory-pools seq)]

      [table/container {:className "inventory-pools"
                        :header (table-head hds)
                        :body (table-body inventory-pools tds)}]
      [:div.alert.alert-info.text-center "No (more) inventory-pools found."])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn debug-component []
  (when (:debug @state/global-state*)
    [:section.debug
     [:hr]
     [:h2 "Page Debug"]
     [:div
      [:h3 "@current-query-parameters-normalized*"]
      [:pre (with-out-str (pprint @current-query-parameters-normalized*))]]
     [:div
      [:h3 "@current-route*"]
      [:pre (with-out-str (pprint @current-route*))]]
     [:div
      [:h3 "@data*"]
      [:pre (with-out-str (pprint @data*))]]]))

(defn page []
  [:<>
   [routing/hidden-state-component
    {:did-change #(fetch)}]

   [:article.inventory-pools

    [:header.my-5
     [:h1
      [icons/warehouse] " Inventory Pools"]]

    [:section
     [filter-section]
     [table/toolbar
      [create/button]]
     [inventory-pools-table]
     [table/toolbar
      [create/button]]]

    [:section
     [create/dialog]
     [debug-component]]]])
