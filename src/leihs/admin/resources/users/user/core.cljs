(ns leihs.admin.resources.users.user.core
  (:require
   [cljs.core.async :as async :refer [<! go timeout]]
   [cljs.pprint :refer [pprint]]
   [clojure.string :refer [trim]]
   [leihs.admin.common.components :as components]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.common.icons :as icons]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.state :as state]
   [leihs.core.auth.core :as auth]
   [leihs.core.core :refer [presence]]
   [leihs.core.routing.front :as routing]
   [reagent.core :as reagent :refer [reaction]]
   [taoensso.timbre :refer [error]]))

(defonce user-id*
  (reaction (or (some-> @routing/state* :route-params :user-id)
                "00000000-0000-0000-0000-000000000000")))

;;; data ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce user-data* (reagent/atom nil))

(defonce fetch-id* (atom nil))

(defn stringify-json [json]
  (.stringify js/JSON (clj->js json)))

(defn clean-and-fetch [& _]
  (reset! user-data* nil)
  (let [fetch-id (reset! fetch-id* (rand-int (js/Math.pow 2 16)))]
    (go (<! (timeout 50))
        (when (= @fetch-id* fetch-id)
          (let [path (path :user {:user-id @user-id*})]
            (reset! user-data*
                    (-> {:chan (async/chan)
                         :url path}
                        http-client/request
                        :chan <! http-client/filter-success! :body
                        (update-in [:extended_info] stringify-json))))))))

(defn modifieable? [current-user-state _]
  (cond
    (auth/system-admin-scopes?
     current-user-state _) true
    (auth/admin-scopes?
     current-user-state
     _)  (cond (or (nil? @user-data*) (:is_system_admin @user-data*)) false
               (or (nil? @user-data*) (:system_admin_protected @user-data*)) false
               :else true)
    :else (cond (or (nil? @user-data*) (:is_admin @user-data*)) false
                (or (nil? user-data*) (:admin_protected @user-data*)) false
                :else true)))

;;; some display helpers ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn fullname [user]
  (-> (str (:firstname user) " " (:lastname user))
      trim
      presence))

(defn some-uid [user]
  (or (some-> user :email presence)
      (some-> user :login presence)
      (some-> user :org_id presence)
      (some-> user :id)))

(defn fullname-or-some-uid [user]
  (or (fullname user) (some-uid user)))

(defn fullname-some-uid-seq [user]
  (->> [(fullname user) (some-uid user)]
       (filter identity)))

(defn name-component [user]
  [:<>
   (when-not user
     (error "use name-link-component when you call wo argument and :user-id is in the routes"))
   (let [path (path :user {:user-id (:id user)})
         name-or-id (fullname-or-some-uid user)]
     [components/link [:<> name-or-id] path])])

(defn name-link-component []
  [:span
   [routing/hidden-state-component
    {:did-mount #(clean-and-fetch)
     :did-change #(clean-and-fetch)}]
   (let [p (path :user {:user-id @user-id*})
         name-or-id (fullname-or-some-uid @user-data*)]
     [components/link [:em name-or-id] p])])

(defn img-avatar-component [user]
  [:div.mb-2
   [components/img-large-component user]])

(defn user-data-li-dl-component [dt dd]
  ^{:key dt}
  [:li {:key dt}
   [:dl.row.mb-0
    [:dt.col-sm-4 dt]
    [:dd.col-sm-8 dd]]])

(defn account-properties-component [user]
  [:ul.list-unstyled
   [user-data-li-dl-component "Enabled"
    (if (:account_enabled user)
      [:span.text-success "yes"]
      [:span.text-danger "no"])]
   [user-data-li-dl-component "Admin"
    (if (:is_admin user)
      [:span "yes"]
      [:span "no"])]
   [user-data-li-dl-component "System-admin"
    (if (:is_system_admin user)
      [:span "yes"]
      [:span "no"])]
   [user-data-li-dl-component "PW sign-in"
    (if (:password_sign_in_enabled user)
      [:span "yes"]
      [:span "no"])]
   [user-data-li-dl-component "Admin protected"
    (if (:admin_protected user)
      [:span "yes"]
      [:span "no"])]
   [user-data-li-dl-component "System-admin protected"
    (if (:system_admin_protected user)
      [:span "yes"]
      [:span "no"])]
   (when-let [badge-id (:badge_id user)]
     [user-data-li-dl-component "Badge ID" badge-id])
   (when-let [login (:login user)]
     [user-data-li-dl-component "Login" login])
   (when-let [org (:organization user)]
     [user-data-li-dl-component "Organization" org])
   (when-let [org-id (:org_id user)]
     [user-data-li-dl-component "Org ID" org-id])
   [user-data-li-dl-component "ID"
    [components/truncated-id-component (:id user)]]])

(defn email-component [email-addr]
  [:a {:href (str "mailto:" email-addr)}
   [icons/email] "\u00A0" [:span  email-addr]])

(defn personal-properties-component [user]
  [:ul.list-unstyled
   [user-data-li-dl-component
    "Name" (fullname user)]
   [user-data-li-dl-component "Email"
    [:ul.list-unstyled
     (for [[idx email] (->> [(:email user)
                             (:secondary_email user)]
                            (map presence)
                            (filter identity)
                            (map-indexed vector))]
       [:li {:key idx} (email-component email)])]]
   (when-let [p (:phone user)]
     [user-data-li-dl-component "Phone" p])
   (let [addr (-> user :address presence)
         zip (-> user :zip presence)
         city (-> user :city presence)
         country (-> user :country presence)]
     (when (or addr zip city country)
       [user-data-li-dl-component "Address"
        [:ul.list-unstyled
         (for [[idx itm]  (some->> [addr (str zip  " "  city) country]
                                   (map str)
                                   (map clojure.string/trim)
                                   (map presence)
                                   (map identity)
                                   (map-indexed vector))]
           ^{:key idx} [:li itm])]]))
   (when-let [url (:url user)]
     [user-data-li-dl-component "URL"
      [:a {:href url}
       [:p
        {:style
         {:white-space :nowrap
          :overflow :hidden
          :text-overflow :ellipsis
          :max-width :15em}}
        url]]])])

(defn debug-component []
  (when (:debug @state/global-state*)
    [:div.user-debug
     [:hr]
     [:div.user-data
      [:h3 "@user-data*"]
      [:pre (with-out-str (pprint @user-data*))]]]))
