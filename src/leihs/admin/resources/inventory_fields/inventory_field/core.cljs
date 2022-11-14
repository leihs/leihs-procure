(ns leihs.admin.resources.inventory-fields.inventory-field.core
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [detect keyword str presence]]
    [leihs.core.log.helpers :refer [log spy]]
    [leihs.core.routing.front :as routing]
    [leihs.core.user.front :as core-user]
    [leihs.core.user.shared :refer [short-id]]
    [leihs.admin.common.icons :as icons]

    [leihs.admin.common.components :as components]
    [leihs.admin.common.http-client.core :as http-client]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.inventory-fields.inventory-field.breadcrumbs :as breadcrumbs]
    [leihs.admin.resources.inventory-fields.inventory-field.specs :as field-specs]
    [leihs.admin.state :as state]

    [accountant.core :as accountant]
    [cljs.core.async :as async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [reagent.core :as reagent]
    ))

(defonce id*
  (reaction (or (-> @routing/state* :route-params :inventory-field-id presence)
                ":inventory-field-id")))

(def new-dynamic-field-defaults field-specs/new-dynamic-field-defaults)
(def simple-types field-specs/simple-types)
(def advanced-types field-specs/advanced-types)

(defonce data* (reagent/atom nil))

(defonce inventory-field-data* (reagent/atom nil))

(defonce inventory-fields-groups-data* (reagent/atom nil))

(defonce inventory-field-usage-data* (reagent/atom nil))

(defonce edit-mode?*
  (reaction
    (and (map? @data*)
         (boolean ((set '(:inventory-field-edit :inventory-field-create))
                   (:handler-key @routing/state*))))))


;;; fetch ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn select-put-keys [data]
  (-> data
      (select-keys field-specs/field-keys)
      (update :data select-keys (if (:dynamic data)
                                  field-specs/dynamic-field-data-keys
                                  field-specs/field-data-keys))))

(defn update-data-values-with-uuid [field]
  (cond-> field
    (some-> field :data :values count (> 0))
    (update-in [:data :values]
               #(vec (map (fn [v] (assoc v :uuid (random-uuid))) %)))))

(defn set-default-uuid [field]
  (cond-> field
    (#{"radio" "select"} (-> field :data :type))
    (assoc-in [:data :default-uuid]
              (:uuid (detect #(= (:value %) (-> field :data :default))
                             (-> field :data :values))))))

(defn fetch []
  (go (let [body (some-> {:chan (async/chan)
                          :url (path :inventory-field
                                     (-> @routing/state* :route-params))}
                         http-client/request :chan <!
                         http-client/filter-success! :body)]
        (reset! inventory-field-data* (:inventory-field-data body))
        (reset! inventory-field-usage-data* (:inventory-field-usage body))
        (reset! data* (-> @inventory-field-data*
                          select-put-keys
                          update-data-values-with-uuid
                          set-default-uuid)))))

(defn fetch-inventory-fields-groups []
  (go (reset! inventory-fields-groups-data*
              (some-> {:chan (async/chan)
                       :url (path :inventory-fields-groups)}
                      http-client/request :chan <!
                      http-client/filter-success!
                      :body :inventory-fields-groups sort))))

(defn clean-and-fetch [& args]
  (reset! data* nil)
  (reset! inventory-fields-groups-data* nil)
  (reset! inventory-field-usage-data* nil)
  (fetch)
  (fetch-inventory-fields-groups))

(defn clean-and-fetch-for-new [& args]
  (reset! data* new-dynamic-field-defaults)
  (reset! inventory-field-data* nil)
  (reset! inventory-fields-groups-data* nil)
  (reset! inventory-field-usage-data* nil)
  (fetch-inventory-fields-groups))

;;; debug ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn debug-component []
  (when (:debug @state/global-state*)
    [:div.field-debug
     [:hr]
     [:div.field-data
      [:h3 "@data*"]
      [:pre (with-out-str (pprint @data*))]]
     [:div.field-data
      [:h3 "@inventory-field-data*"]
      [:pre (with-out-str (pprint @inventory-field-data*))]]
     [:div.inventory-fields-groups-data
      [:h3 "@inventory-field-usage-data*"]
      [:pre (with-out-str (pprint @inventory-field-usage-data*))]]
     [:div.inventory-fields-groups-data
      [:h3 "@inventory-fields-groups-data*"]
      [:pre (with-out-str (pprint @inventory-fields-groups-data*))]]]))


;;; components ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn id-component []
  [:p "id: " [:span {:style {:font-family "monospace"}} @id*]])

(defn id-link-component []
  [:span
   [routing/hidden-state-component
    {:did-change fetch}]
   (let [p (path :inventory-field {:inventory-field-id @id*})
         inner (if @data*
                 [:em (str (:id @inventory-field-data*))]
                 [:span {:style {:font-family "monospace"}} (short-id @id*)])]
     [components/link inner p])])
