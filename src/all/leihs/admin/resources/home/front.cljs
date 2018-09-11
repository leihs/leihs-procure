(ns leihs.admin.resources.home.front
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.password-authentication.front :as password-authentication]
    [leihs.core.requests.core :as requests]
    [leihs.core.routing.front :as routing]
    [leihs.core.user.front :as core-user]

    [leihs.admin.front.breadcrumbs :as breadcrumbs]
    [leihs.admin.front.state :as state]
    [leihs.admin.paths :refer [path]]

    [accountant.core :as accountant]
    [cljs.core.async :as async]
    [cljs.pprint :refer [pprint]]
    [reagent.core :as reagent]
    ))

(def password-sign-in-data* (reagent/atom {}))

(defn sign-in []
  (def sign-in-id* (reagent/atom nil))
  (let [resp-chan (async/chan)
        id (requests/send-off {:url (path :password-authentication)
                               :method :post
                               :json-params @password-sign-in-data*}
                              {:modal true
                               :title "Sign in with password"
                               :retry-fn #'sign-in}
                              :chan resp-chan)]
    (reset! sign-in-id* id)
    (go (let [resp (<! resp-chan)]
          (when (and (= (:status resp) 200)
                     (= id @sign-in-id*))
            (reset! core-user/state* (:body resp))
            (accountant/navigate! (path :admin)))))))

(defn password-sign-in-form []
  [:div
   [:form.form
    {:on-submit (fn [e]
                  (.preventDefault e)
                  (sign-in))}
    [:div.form-group
     [:label " email "]
     [:input#email.form-control
      {:type :email
       :value (-> @password-sign-in-data* :email)
       :on-change #(swap! password-sign-in-data* assoc :email (-> % .-target .-value))
       :auto-complete :email}]]
    [:div.form-group
     [:label " password "]
     [:input#password.form-control
      {:type :password
       :value (-> @password-sign-in-data* :password)
       :on-change #(swap! password-sign-in-data* assoc :password (-> % .-target .-value))
       :auto-complete :current-password}]]
    [:div.form-group.float-right
     [:button.btn.btn-primary
      {:type :submitt}
      "Sign in with password"]]]
   [:div.clearfix]
   [:pre (with-out-str (pprint @password-sign-in-data*))]])


(defn page []
  [:div.home
   (when-let [user @core-user/state*]
     (breadcrumbs/nav-component
       [(breadcrumbs/leihs-li)]
       [(breadcrumbs/admin-li)
        (breadcrumbs/borrow-li)
        (breadcrumbs/lending-li)
        (breadcrumbs/procurement-li)]))
   [:h1 "leihs-admin Home"]
   [:p.text-danger "This page is only accessible for development and testing."]
   ])
