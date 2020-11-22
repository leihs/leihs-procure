(ns leihs.admin.resources.users.user.core
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.requests.core :as requests]
    [leihs.core.routing.front :as routing]
    [leihs.core.icons :as icons]
    [leihs.core.auth.core :as auth]

    [leihs.admin.state :as state]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.common.components :as components]
    [leihs.admin.resources.users.user.shared :as user-shared]

    [cljs.pprint :refer [pprint]]
    [cljs.core.async :as async :refer [timeout]]
    [clojure.string :refer [split trim]]
    [reagent.core :as reagent]
    [taoensso.timbre :as logging]
    ))


(defonce user-id*
  (reaction (or  (-> @routing/state* :route-params :user-id)
                ":user-id")))


;;; data ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce user-data* (reagent/atom nil))

(defn stringify-json [json]
  (.stringify js/JSON (clj->js json)))

(defn clean-and-fetch
  [& {:keys [path]
      :or {path (path :user {:user-id @user-id*})}}]
  (def fetch-id* (reagent/atom nil))
  (reset! user-data* nil)
  (let [resp-chan (async/chan)
        id (requests/send-off {:url path
                               :method :get
                               :query-params {}}
                              {:modal false
                               :title "Fetch User"}
                              :chan resp-chan)]
    (reset! fetch-id* id)
    (go (let [resp (<! resp-chan)]
          (when (and (= (:status resp) 200)
                     (= id @fetch-id*))
            (reset! user-data*
                    (-> resp :body (update-in [:extended_info]
                                              stringify-json))))))))



;;; some display helpers ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn fullname [user]
  (-> (str (:firstname user) " " (:lastname user))
      trim
      presence))

(defn some-id [user]
  [:span.text-monospace
   (or (some-> user :email presence)
       (some-> user :login presence)
       (some-> user :org_id presence)
       (some-> user :id (split "-") first))])

(defn fullname-or-some-id [user]
  (or (fullname user) (some-id user)))

(defn fullname-some-id-seq [user]
  (->> [(fullname user) (some-id user)]
       (filter identity)))

(defn name-component [user]
  [:span
   (when-not user
     (logging/error "use name-link-component when you call wo argument and :user-id is in the routes"))
   (let [p (path :user {:user-id (:id user)})
         name-or-id (fullname-or-some-id user)]
     [components/link [:em name-or-id] p])])

(defn name-link-component []
  [:span
   [routing/hidden-state-component
    {:did-change clean-and-fetch
     :did-mount clean-and-fetch}]
   (let [p (path :user {:user-id @user-id*})
         name-or-id (fullname-or-some-id @user-data*)]
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
   [user-data-li-dl-component "PW sign-in"
    (if (:password_sign_in_enabled user)
      [:span "yes"]
      [:span "no"])]
   [user-data-li-dl-component "Protected"
    (if (:protected user)
      [:span "yes"]
      [:span "no"])]
   [user-data-li-dl-component "Admin"
    (if (:is_admin user)
      [:span "yes"]
      [:span "no"])]
   (when-let [login (:login user)]
     [user-data-li-dl-component "Login" login])
   (when-let [org-id (:org_id user)]
     [user-data-li-dl-component "Org ID" org-id])
   (when-let [badge-id (:badge_id user)]
     [user-data-li-dl-component "Badge ID" badge-id])
   [user-data-li-dl-component "ID" (:id user)]
   ])

(defn email-component [email-addr]
  [:a {:href (str "mailto:" email-addr)}
   icons/email "\u00A0" [:span  email-addr ]])

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
                                   (map clojure.string/trim)
                                   (map presence)
                                   (map identity)
                                   (map-indexed vector))]
           ^{:key idx} [:li itm])]]))
   (when-let [url (:url user)]
     [user-data-li-dl-component "URL" [:a {:href url} url]])])


(defn debug-component []
  (when (:debug @state/global-state*)
    [:div.user-debug
     [:hr]
     [:div.user-data
      [:h3 "@user-data*"]
      [:pre (with-out-str (pprint @user-data*))]]]))




