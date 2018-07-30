(ns leihs.admin.resources.user.front
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.admin.front.breadcrumbs :as breadcrumbs]
    [leihs.admin.front.components :as components]
    [leihs.admin.front.requests.core :as requests]
    [leihs.admin.front.shared :refer [humanize-datetime-component short-id gravatar-url]]
    [leihs.admin.front.state :as state]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.utils.core :refer [keyword str presence]]

    [accountant.core :as accountant]
    [cljs.core.async :as async]
    [cljs.core.async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [cljsjs.jimp]
    [cljsjs.moment]
    [clojure.contrib.inflect :refer [pluralize-noun]]
    [reagent.core :as reagent]
    ))

(declare fetcher-user)
(defonce user-id* (reaction (-> @state/routing-state* :route-params :user-id)))
(defonce user-data* (reagent/atom nil))


(defonce edit-mode?*
  (reaction
    (and (map? @user-data*)
         (boolean ((set '(:user-edit :user-new))
                   (:handler-key @state/routing-state*))))))

(def fetch-user-id* (reagent/atom nil))
(defn fetch-user []
  (let [resp-chan (async/chan)
        id (requests/send-off {:url (path :user (-> @state/routing-state* :route-params))
                               :method :get
                               :query-params {}}
                              {:modal false
                               :title "Fetch User"
                               :handler-key :user
                               :retry-fn #'fetch-user}
                              :chan resp-chan)]
    (reset! fetch-user-id* id)
    (go (let [resp (<! resp-chan)]
          (when (and (= (:status resp) 200)
                     (= id @fetch-user-id*))
            (reset! user-data* (:body resp)))))))


;;; reload logic ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn clean-and-fetch [& args]
  (reset! user-data* nil)
  (fetch-user))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn field-component
  ([kw]
   (field-component kw {}))
  ([kw opts]
   (let [opts (merge {:type :text} opts)]
     (when (or @edit-mode?* (not= kw :password))
       [:div.form-group.row
        [:label.col.col-form-label.col-sm-2 {:for kw} kw]
        [:div.col.col-sm-10
         [:div.input-group
          (if @edit-mode?*
            [:input.form-control
             {:id kw
              :type (:type opts)
              :value (or (kw @user-data*) "")
              :on-change #(swap! user-data* assoc kw (-> % .-target .-value presence))
              :disabled (not @edit-mode?*)}]
            [:div
             (if-let [value (-> @user-data* kw presence)]
               [:span.form-control-plaintext.text-truncate
                {:style
                 {:max-width "20em"}}
                (case (:type opts)
                  :email [:a {:href (str "mailto:" value)}
                          [:i.fas.fa-envelope] " " value]
                  :url [:a {:href value} value]
                  value)])])]]]))))

(defn checkbox-component [kw]
  [:div.form-check.form-check-inline
   [:label {:for kw}
    [:input
     {:id kw
      :type :checkbox
      :checked (kw @user-data*)
      :on-change #(swap! user-data* assoc kw (-> @user-data* kw boolean not))
      :disabled (not @edit-mode?*)}]
    [:span.ml-2 kw]]])

(defn debug-component []
  (when (:debug @state/global-state*)
    [:div.user-debug
     [:hr]
     [:div.edit-mode?*
      [:h3 "@edit-mode?*"]
      [:pre (with-out-str (pprint @edit-mode?*))]]
     [:div.user-data
      [:h3 "@user-data*"]
      [:pre (with-out-str (pprint @user-data*))]]]))


;;; image ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def img-processing* (reagent/atom {}))

(defn allow-drop [e] (.preventDefault e))

(defn get-img-data [dataTransfer array-buffer-handler]
  (js/console.log (clj->js ["TODO" 'get-img-data dataTransfer]))
  (let [url (.getData dataTransfer "text/plain")]
    (js/console.log (clj->js ["URL" url]))
    (js/console.log (clj->js ["DATA" (.getData dataTransfer "text/uri-list")]))
    (js/console.log (clj->js ["DATA" (.getData dataTransfer "text/html")]))
    (js/console.log (clj->js ["ITEMS" (.-items dataTransfer)]))
    (js/console.log (clj->js ["TYPES" (.-types dataTransfer)]))
    ))

(defn get-file-data [dataTransfer array-buffer-handler]
  (let [f (aget (.-files dataTransfer) 0)
        fname (.-name f)
        reader (js/FileReader.)]
    (set! (.-onload reader)
          (fn [e]
            (let [data (-> e .-target .-result)]
              (array-buffer-handler data))))
    (.readAsArrayBuffer reader f)))

(defn img-handler [data]
  (doseq [res [256 32]]
    (js/Jimp.read
      data (fn [err img]
             (when err
               (swap! img-processing* assoc :error err)
               (throw err))
             (doto img
               (.resize res res)
               (.quality 80))
             (.getBase64 img "image/jpeg"
                         (fn [err b64]
                           (if err
                             (swap! img-processing* assoc :error err)
                             (swap! user-data* assoc (keyword (str "img" res "_url")) b64))))))))

(defn handle-img-drop [evt]
  (reset! img-processing* {})
  (do
    (allow-drop evt)
    (.stopPropagation evt)
    (let [data-transfer (.. evt -dataTransfer)]
      (if (< 0 (-> data-transfer .-files .-length))
        (get-file-data data-transfer img-handler)
        (get-img-data data-transfer img-handler)))))

(defn handle-img-chosen [evt]
  (reset! img-processing* {})
  (js/console.log evt)
  (js/console.log (-> evt .-target .-files .-length))
  (get-file-data (-> evt .-target) img-handler))

(defn file-upload []
  [:div.box.mb-2
   {:style {:position :relative}
    :on-drag-over #(allow-drop %)
    :on-drop #(handle-img-drop %)
    :on-drag-enter #(allow-drop %)}
   [:div.text-center
    {:style
     {:position :relative
      :width "256px"
      :height "256px"}}
    [:div.pt-2
     [:label.btn.btn-sm.btn-dark
      [:i.fas.fa-file-image]
      " Choose file "
      [:input#user-image.sr-only
       {:type :file
        :on-change handle-img-chosen}]]
     [:p "or drop file image here"]]
    [:div.text-center
     {:style {:position :absolute
              :bottom 0
              :width "100%"}}
     [:div
      (when (:img256_url @user-data*)
        [:p {:style {:margin-top "1em"}}
         [:button.btn.btn-sm.btn-dark
          {:on-click #(swap! user-data* assoc :img256_url nil :img32_url nil)}
          [:i.fas.fa-times] " Remove image "]])]]]
   (if-let [img-data (:img256_url @user-data*)]
           [:img {:width 256
                  :height 256
                  :src img-data
                  :style {:position :absolute
                          :left 0
                          :top 0
                          :width "256px"
                          :height "256px"
                          :opacity 0.3
                          :z-index -1}}]
           [:div.bg-light
            {:style {:position :absolute
                     :left 0
                     :top 0
                     :width "256px"
                     :height "256px"
                     :z-index -1 }}])])

(defn image-component []
  [:div.clearfix
   [:h3 "User-Image"]
   (if-not @edit-mode?*
     [:img.bg-light.user-image-256.mb-2
      {:src (if-let [data (:img256_url @user-data*)]
              data
              (gravatar-url (:email @user-data*) 256))
       :width 256
       :height 256}]
     [file-upload])
   [field-component :img256_url {:type :url}]
   [field-component :img32_url {:type :url}]])


;; user components ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn basic-component []
  [:div.form.mt-2
   [:h3 "Basic properties"]
   [checkbox-component :is_admin]
   [checkbox-component :account_enabled]
   [checkbox-component :password_sign_in_enabled]
   [field-component :firstname]
   [field-component :lastname]
   [field-component :phone]
   [field-component :email {:type :email}]])

(defn address-component []
  [:div.form.mt-2
   [:h3 "Address"]
   [field-component :address]
   [field-component :zip]
   [field-component :city]
   [field-component :country]
   ])

(defn rest-component []
  [:div.form.mt-2
   [:h3 "Other"]
   [field-component :url {:type :url}]
   [field-component :org_id]
   [field-component :badge_id]
   [field-component :password {:type :password}]])

(defn additional-properties-component []
  [:div.additional-properties
   [:p [:span "The user has been created " [humanize-datetime-component (:created_at @user-data*)]
        ", and updated "[humanize-datetime-component (:updated_at @user-data*)]
        ". "]
    (let [c (:contracts_count @user-data*)]
      (if (< c 1)
        [:span "The user has no contracts."]
        [:span "The user has " c " " (pluralize-noun c "contract")  ". "]))
    (let [c (:inventory_pool_roles_count @user-data*)]
      (if (< c 1)
        [:span "The user has no inventory pool roles."]
        [:span "The user has " 
         [:a {:href (path :user-inventory-pools-roles {:user-id @user-id*})}
          c " inventory pool " (pluralize-noun c "role")]  ". "]))]])

(defn user-component []
  [:div.user-component
   (if (nil?  @user-data*)
     [:div.text-center
      [:i.fas.fa-spinner.fa-spin.fa-5x]
      [:span.sr-only "Please wait"]]
     [:div
      [basic-component]
      [image-component]
      [address-component]
      [rest-component]
      (when-not @edit-mode?*
        [additional-properties-component])])])

(defn user-name-component []
  (if-not @user-data*
    [:span {:style {:font-family "monospace"}} (short-id @user-id*)]
    [:em (str (:firstname @user-data*) " " (:lastname @user-data*))]))

(defn user-id-component []
  [:p "id: " [:span {:style {:font-family "monospace"}} (:id @user-data*)]])

;;; show ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn show-page []
  [:div.user
   [state/hidden-routing-state-component
    {:will-mount clean-and-fetch
     :did-change clean-and-fetch}]
   (breadcrumbs/nav-component
     [(breadcrumbs/leihs-li)
      (breadcrumbs/admin-li)
      (breadcrumbs/users-li)
      (breadcrumbs/user-li @user-id*)]
     [(breadcrumbs/api-tokens-li @user-id*)
      (breadcrumbs/user-delete-li @user-id*)
      (breadcrumbs/user-edit-li @user-id*)
      (breadcrumbs/email-li (:email @user-data*))])
   [:div.row
    [:div.col-lg
     [:h1
      [:span " User "]
      [user-name-component]]
     [user-id-component]]]
   [user-component]
   [debug-component]])


;;; inventory pool roles ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def inventory-pools-roles-data* (reagent/atom nil))

(defn prepare-inventory-pools-data [data]
  (->> data
       (reduce (fn [roles role]
                 (-> roles
                     (assoc-in [(:inventory_pool_id role) :name] (:inventory_pool_name role))
                     (assoc-in [(:inventory_pool_id role) :id] (:inventory_pool_id role))
                     (assoc-in [(:inventory_pool_id role) :key] (:inventory_pool_id role))
                     (assoc-in [(:inventory_pool_id role) :roles (:role role)] role)))
               {})
       (map (fn [[_ v]] v))
       (sort-by :name)
       (into [])))

(defonce fetch-inventory-pools-roles-id* (reagent/atom nil))

(defn fetch-inventory-pools-roles []
  (let [resp-chan (async/chan)
        id (requests/send-off {:url (path :user-inventory-pools-roles 
                                          (-> @state/routing-state* :route-params))
                               :method :get
                               :query-params {}}
                              {:modal false
                               :title "Fetch Inventory-Pools-Roles"
                               :retry-fn #'fetch-inventory-pools-roles}
                              :chan resp-chan)]
    (reset! fetch-inventory-pools-roles-id* id)
    (go (let [resp (<! resp-chan)]
          (when (and (= (:status resp) 200)
                     (= id @fetch-inventory-pools-roles-id*))
            (reset! inventory-pools-roles-data* 
                    (-> resp :body :inventory_pools_roles prepare-inventory-pools-data)))))))

(defn clean-and-fetch-inventory-pools-roles [& args]
  (clean-and-fetch)
  (reset! inventory-pools-roles-data* nil)
  (fetch-inventory-pools-roles))

(defn inventory-pools-roles-debug-component []
  [:div
   (when (:debug @state/global-state*)
     [:div.inventory-pools-roles-debug
      [:hr]
      [:div.inventory-pools-roles-data
       [:h3 "@inventory-pools-roles-data*"]
       [:pre (with-out-str (pprint @inventory-pools-roles-data*))]]])
   [debug-component]])

(defn inventory-pools-roles-page []
  [:div.user-inventory-pools-roles
   [state/hidden-routing-state-component
    {:will-mount clean-and-fetch-inventory-pools-roles
     :did-change clean-and-fetch-inventory-pools-roles}]
   (breadcrumbs/nav-component
     [(breadcrumbs/leihs-li)
      (breadcrumbs/admin-li)
      (breadcrumbs/users-li)
      (breadcrumbs/user-li @user-id*)
      (breadcrumbs/user-inventory-pools-rooles-li @user-id*)]
     [])
   [:div.row
    [:div.col-lg
     [:h1
      [:span " User "]
      [user-name-component]
      " Inventory-Pools-Roles"]
     [user-id-component]]]
   [:div.roles
    [:h1 "Active Roles"]
    (for [pool @inventory-pools-roles-data*]
      [:div.pool {:key (:id pool)}
       [:h2 "Pool \"" (:name pool) "\""]
       [:ul
        (for [[_ role] (:roles pool)]
          [:li {:key (:id role)} (:role role)])]])]
   [inventory-pools-roles-debug-component]])


;;; edit ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn patch [_]
  (let [resp-chan (async/chan)
        id (requests/send-off {:url (path :user {:user-id @user-id*})
                               :method :patch
                               :json-params  @user-data*}
                              {:modal true
                               :title "Update User"
                               :handler-key :user-edit
                               :retry-fn #'patch}
                              :chan resp-chan)]
    (go (let [resp (<! resp-chan)]
          (when (= (:status resp) 204)
            (accountant/navigate!
              (path :user {:user-id @user-id*})))))))

(defn patch-submit-component []
  (if @edit-mode?*
    [:div
     [:div.float-right
      [:button.btn.btn-warning
       {:on-click patch}
       [:i.fas.fa-save]
       " Save "]]
     [:div.clearfix]]))

(defn edit-page []
  [:div.edit-user
   [state/hidden-routing-state-component
    {:will-mount clean-and-fetch
     :did-change clean-and-fetch}]
   (breadcrumbs/nav-component
     [(breadcrumbs/leihs-li)
      (breadcrumbs/admin-li)
      (breadcrumbs/users-li)
      (breadcrumbs/user-li @user-id*)
      (breadcrumbs/user-edit-li @user-id*)][])
   [:div.row
    [:div.col-lg
     [:h1
      [:span " Edit User "]
      [user-name-component]]
     [user-id-component]]]
   [user-component]
   [patch-submit-component]
   [debug-component]])


;;; new  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn create [_]
  (let [resp-chan (async/chan)
        id (requests/send-off {:url (path :users)
                               :method :post
                               :json-params  @user-data*}
                              {:modal true
                               :title "Create User"
                               :handler-key :user-new
                               :retry-fn #'create}
                              :chan resp-chan)]
    (go (let [resp (<! resp-chan)]
          (when (= (:status resp) 200)
            (accountant/navigate!
              (path :user {:user-id (-> resp :body :id)})))))))

(defn create-submit-component []
  (if @edit-mode?*
    [:div
     [:div.float-right
      [:button.btn.btn-primary
       {:on-click create}
       " Create "]]
     [:div.clearfix]]))

(defn new-page []
  [:div.new-user
   [state/hidden-routing-state-component
    {:will-mount #(reset! user-data* {:account_enabled true
                                      :password_sign_in_enabled true})}]
   (breadcrumbs/nav-component
     [(breadcrumbs/leihs-li)
      (breadcrumbs/admin-li)
      (breadcrumbs/users-li)
      (breadcrumbs/user-add-li)
      ][])
   [:div.row
    [:div.col-lg
     [:h1
      [:span " New User "]]]]
   [user-component]
   [create-submit-component]
   [debug-component]])


;;; delete ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def transfer-data* (reagent/atom {}))

(defn delete-user [_]
  (let [resp-chan (async/chan)
        id (requests/send-off {:url (path :user (-> @state/routing-state* :route-params))
                               :method :delete
                               :query-params {}}
                              {:title "Delete User"
                               :handler-key :user-delete
                               :retry-fn #'delete-user}
                              :chan resp-chan)]
    (go (let [resp (<! resp-chan)]
          (when (= (:status resp) 204)
            (accountant/navigate!
              (path :users {}
                    (-> @state/global-state* :users-query-params))))))))

(defn delete-without-reasignment-component []
  [:div.card.m-3
   [:div.card-header.bg-warning
    [:h2 "Delete User"]]
   [:div.card-body
    [:p
     "Deleting this user is not possible if it is associated with contracts, reserverations, or orders. "
     "If this is the case this operation will fail without deleting or even changing any data. "]
    [:p.text-danger
     "Permissions, such as given by delegations, groups, or roles will not prevent deletion of this user. " ]
    [:div.float-right
     [:button.btn.btn-warning.btn-lg
      {:on-click delete-user}
      [:i.fas.fa-times] " Delete"]]]])

(defn transfer-data-and-delete-user [_]
  (let [resp-chan (async/chan)
        url (path :user-transfer-data
                  {:user-id @user-id*
                   :target-user-id (:target-user-id @transfer-data*)})
        id (requests/send-off
             {:url url
              :method :delete
              :query-params {}}
             {:title "Transfer Data and Delete User"
              :handler-key :user-delete
              :retry-fn #'transfer-data-and-delete-user}
             :chan resp-chan)]
    (go (let [resp (<! resp-chan)]
          (when (= (:status resp) 204)
            (accountant/navigate!
              (path :users {}
                    (-> @state/global-state* :users-query-params))))))))

(defn delete-with-transfer-component []
  [:div.card.m-3
   [:div.card-header.bg-danger
    [:h2 "Transfer Data and Delete User"]]
   [:div.card-body
    [:p
     "Contracts, reserverations, and orders of this user will be "
     "transferred to the user entered below. " ]
    [:p.text-danger
     "Permissions, such as given by delegations, groups, or roles will not be 
     transferred! " ]
    [:p.text-danger
     "Audits will still contain references to the removed user! "]
    [:p.text-danger
     "External data, such as open contracts printed on paper for example, "
     "will become inconsistent with the data in leihs!"]
    [:div.form
     [:div.form-group
      [:label {:for :target-user-id} "Target user id" ]
      [:input#target-user-id.form-control
       {:type :text
        :value (or (-> @transfer-data* :target-user-id) "")
        :on-change #(swap! transfer-data* assoc
                           :target-user-id (-> % .-target .-value presence))}]]]
    [:div.float-right
     [:button.btn.btn-danger
      {:on-click transfer-data-and-delete-user}
      [:i.fas.fa-times] " Transfer and delete"]]]])

(defn delete-page []
  [:div.user-delete
   [state/hidden-routing-state-component
    {:will-mount clean-and-fetch
     :did-change clean-and-fetch}]
   [:div.row
    [:nav.col-lg {:aria-label :breadcrumb :role :navigation}
     [:ol.breadcrumb
      [breadcrumbs/leihs-li]
      [breadcrumbs/admin-li]
      [breadcrumbs/users-li]
      [breadcrumbs/user-li @user-id*]
      [breadcrumbs/user-delete-li @user-id*]]]
    [:nav.col-lg {:role :navigation}]]
   [:h1 "Delete User "
    [user-name-component]]
   [user-id-component]
   [delete-without-reasignment-component]
   [delete-with-transfer-component]])
