(ns leihs.admin.resources.system.authentication-systems.authentication-system.main
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.requests.core :as requests]
    [leihs.core.routing.front :as routing]
    [leihs.core.user.shared :refer [short-id]]
    [leihs.core.url.shared]

    [leihs.admin.common.components :as components]
    [leihs.admin.common.form-components :as form-components]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.system.authentication-systems.authentication-system.breadcrumbs :as breadcrumbs]
    [leihs.admin.resources.system.authentication-systems.breadcrumbs :as parent-breadcrumbs]
    [leihs.admin.state :as state]
    [leihs.admin.utils.misc :refer [wait-component]]
    [leihs.core.icons :as icons]

    [accountant.core :as accountant]
    [cljs.core.async :as async]
    [cljs.core.async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [clojure.contrib.inflect :refer [pluralize-noun]]
    [reagent.core :as reagent]
    ))

(defonce id*
  (reaction (or (-> @routing/state* :route-params :authentication-system-id)
                ":authentication-system-id")))
(defonce authentication-system-data* (reagent/atom nil))


(defonce edit-mode?*
  (reaction
    (and (map? @authentication-system-data*)
         (boolean ((set '(:authentication-system-edit :authentication-system-create))
                   (:handler-key @routing/state*))))))

(defn fetch [& args]
  (defonce fetch-id* (reagent/atom nil))
  (let [resp-chan (async/chan)
        id (requests/send-off {:url (path :authentication-system (-> @routing/state* :route-params))
                               :method :get
                               :query-params {}}
                              {:modal false
                               :title "Fetch Authentication-System"
                               :handler-key :authentication-system
                               :retry-fn #'fetch}
                              :chan resp-chan)]
    (reset! fetch-id* id)
    (go (let [resp (<! resp-chan)]
          (when (and (= (:status resp) 200)
                     (= id @fetch-id*))
            (reset! authentication-system-data* (:body resp)))))))


;;; reload logic ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn clean-and-fetch [& args]
  (reset! authentication-system-data* nil)
  (fetch))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn debug-component []
  (when (:debug @state/global-state*)
    [:div.authentication-system-debug
     [:hr]
     [:div.edit-mode?*
      [:h3 "@edit-mode?*"]
      [:pre (with-out-str (pprint @edit-mode?*))]]
     [:div.authentication-system-data
      [:h3 "@authentication-system-data*"]
      [:pre (with-out-str (pprint @authentication-system-data*))]]]))


;; authentication-system components ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn basic-component []
  [:div.form.mt-2
   [:h3 "Properties"]

   [:div.form-row
    [:div.col-md-3
     [form-components/input-component authentication-system-data* [:id]
      :label "Id"
      :disabled (or (not= (-> @routing/state* :handler-key) :authentication-system-create)
                    (not @edit-mode?*))]]
    [:div.col-md-5
     [form-components/input-component authentication-system-data* [:name]
      :disabled (not @edit-mode?*)
      :label "Name"]]
    [:div.col-md-4
     [form-components/input-component authentication-system-data* [:type]
      :disabled (not @edit-mode?*)
      :label "Type"]]]

   [:div.form-row
    [:div.col-md-2
     [form-components/input-component authentication-system-data* [:priority]
      :disabled (not @edit-mode?*)
      :label "Priority"]]
    [:div.col-md-2
     [form-components/checkbox-component authentication-system-data* [:enabled]
      :disabled (not @edit-mode?*)
      :label "Enabled"]]
    [:div.col-md-2
     [form-components/checkbox-component authentication-system-data* [:send_email]
      :disabled (not @edit-mode?*)
      :label "Send email-address"]]
    [:div.col-md-2
     [form-components/checkbox-component authentication-system-data* [:send_org_id]
      :disabled (not @edit-mode?*)
      :label "Send org_id"]]
    [:div.col-md-2
     [form-components/checkbox-component authentication-system-data* [:send_login]
      :disabled (not @edit-mode?*)
      :label "Send login"]]]

   [:div.form-row
    [:div.col
     [form-components/input-component authentication-system-data* [:description]
      :disabled (not @edit-mode?*)
      :element :textarea
      :rows 5
      :label "Description"]]]

   [:div.form-row
    [:div.col-md-6
     [form-components/input-component authentication-system-data* [:external_sign_in_url]
      :disabled (not @edit-mode?*)
      :label "External sign-in URL"]]
    [:div.col-md-6
     [form-components/input-component authentication-system-data* [:external_sign_out_url]
      :disabled (not @edit-mode?*)
      :label "External sign-out URL"]]]

   [form-components/input-component authentication-system-data* [:internal_private_key]
    :disabled (not @edit-mode?*)
    :element :textarea
    :rows 2
    :label "Internal private key"]

   [form-components/input-component authentication-system-data* [:internal_public_key]
    :disabled (not @edit-mode?*)
    :element :textarea
    :rows 2
    :label "Internal public key"]

   [form-components/input-component authentication-system-data* [:external_public_key]
    :disabled (not @edit-mode?*)
    :element :textarea
    :rows 2
    :label "External public key"]

   ])

(defn count-component [kw noun data]
  (let [count (kw data)]
    [:span
     "This authentication-system has "
     [:strong count " " (pluralize-noun count noun)] ". "]))

(defn authentication-system-component []
  [:div.authentication-system-component
   (if (nil?  @authentication-system-data*)
     [wait-component]
     [:div
      [:div [basic-component]]])])

(defn name-component []
  [:span
   [routing/hidden-state-component
    {:did-change fetch}]
   (let [p (path :authentication-system {:authentication-system-id @id*})
         inner (if-not @authentication-system-data*
                 [:span {:style {:font-family "monospace"}} (short-id @id*)]
                 [:em (str (:name @authentication-system-data*))])]
     [components/link inner p])])

(defn authentication-system-id-component []
  [:p "id: " [:span {:style {:font-family "monospace"}} (:id @authentication-system-data*)]])


;;; show ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn show-breadcrumbs []
  [breadcrumbs/nav-component
   @breadcrumbs/left*
   [[breadcrumbs/users-li]
    [breadcrumbs/groups-li]
    [breadcrumbs/delete-li]
    [breadcrumbs/edit-li]]])

(defn show-page []
  [:div.authentication-system
   [show-breadcrumbs]
   [:div.row
    [:div.col-lg
     [:h1
      [:span " Authentication-System "]
      [name-component]]
     [authentication-system-id-component]]]
   [authentication-system-component]
   [debug-component]])


;;; edit ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn patch [& args]
  (let [resp-chan (async/chan)
        id (requests/send-off {:url (path :authentication-system {:authentication-system-id @id*})
                               :method :patch
                               :json-params  (dissoc @authentication-system-data* :users_count)}
                              {:modal true
                               :title "Update Authentication-System"
                               :handler-key :authentication-system-edit
                               :retry-fn #'patch}
                              :chan resp-chan)]
    (go (let [resp (<! resp-chan)]
          (when (= (:status resp) 204)
            (accountant/navigate!
              (path :authentication-system {:authentication-system-id @id*})))))))

(defn edit-form []
  [:form.form
   {:on-submit (fn [e]
                 (.preventDefault e)
                 (patch))}
   [authentication-system-component]
   [form-components/save-submit-component]])

(defn edit-page []
  [:div.edit-authentication-system
   [routing/hidden-state-component
    {:did-mount clean-and-fetch
     :did-change clean-and-fetch}]
   [breadcrumbs/nav-component
    (conj @breadcrumbs/left* [breadcrumbs/edit-li]) []]
   [:div.row
    [:div.col-lg
     [:h1
      [:span " Edit Authentication-System "]
      [name-component]]
     [authentication-system-id-component]]]
   [edit-form]
   [debug-component]])


;;; create  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn create [& args]
  (let [resp-chan (async/chan)
        id (requests/send-off {:url (path :authentication-systems)
                               :method :post
                               :json-params  @authentication-system-data*}
                              {:modal true
                               :title "Create Authentication-System"
                               :handler-key :authentication-system-create
                               :retry-fn #'create}
                              :chan resp-chan)]
    (go (let [resp (<! resp-chan)]
          (when (= (:status resp) 200)
            (accountant/navigate!
              (path :authentication-system {:authentication-system-id (-> resp :body :id)})))))))

(defn create-form []
  [:form
   {:on-submit (fn [e]
                 (.preventDefault e)
                 (create))}
   [authentication-system-component]
   [form-components/create-submit-component]])

(defn create-page []
  [:div.new-authentication-system
   [routing/hidden-state-component
    {:did-mount #(reset! authentication-system-data* {})}]
   [breadcrumbs/nav-component
    (conj @parent-breadcrumbs/left* [parent-breadcrumbs/create-li])[]]
   [:div.row
    [:div.col-lg
     [:h1
      [:span " Create Authentication-System "]]]]
   [create-form]
   [debug-component]])


;;; delete ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn delete-authentication-system [& args]
  (let [resp-chan (async/chan)
        id (requests/send-off {:url (path :authentication-system (-> @routing/state* :route-params))
                               :method :delete
                               :query-params {}}
                              {:title "Delete Authentication-System"
                               :handler-key :authentication-system-delete
                               :retry-fn #'delete-authentication-system}
                              :chan resp-chan)]
    (go (let [resp (<! resp-chan)]
          (when (= (:status resp) 204)
            (accountant/navigate!
              (path :authentication-systems {}
                    (-> @state/global-state* :authentication-systems-query-params))))))))

(defn delete-form []
  [:form.form
   {:on-submit (fn [e]
                 (.preventDefault e)
                 (delete-authentication-system))}
   [form-components/delete-submit-component]])

(defn delete-page []
  [:div.authentication-system-delete
   [routing/hidden-state-component
    {:did-mount clean-and-fetch
     :did-change clean-and-fetch}]
   [breadcrumbs/nav-component
    (conj @breadcrumbs/left* [breadcrumbs/delete-li]) []]
   [:h1 "Delete Authentication-System "
    [name-component]]
   [authentication-system-id-component]
   [delete-form]])
