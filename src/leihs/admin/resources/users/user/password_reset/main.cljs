(ns leihs.admin.resources.users.user.password-reset.main
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    )
  (:require
    ["date-fns" :as date-fns]
    [accountant.core :as accountant]
    [cljs.core.async :as async :refer [go <! timeout]]
    [cljs.pprint :refer [pprint]]
    [leihs.admin.common.form-components :as forms]
    [leihs.admin.common.http-client.core :as http-client]
    [leihs.admin.common.icons :as icons]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.users.user.breadcrumbs :as breadcrumbs]
    [leihs.admin.resources.users.user.core :as core :refer [user-id* user-data*]]
    [leihs.admin.state :as state]
    [leihs.admin.utils.misc :as front-shared :refer [wait-component]]
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.routing.front :as routing]
    [leihs.core.url.core :as url]
    [qrcode.react :as qrcode-recat]
    [reagent.core :as reagent]
    [taoensso.timbre :refer [error warn info debug spy]]
    ))


(def user-password-resetable?*
  (reaction
    (and (-> @user-data* :account_enabled)
         (-> @user-data* :password_sign_in_enabled)
         (or (-> @user-data* :email presence)
             (-> @user-data* :login presence)))))

(def data-defaults {:valid_for_hours 24
                    :mode :create})

(defn submit [data*]
  (go (let [res (some->
                  {:chan (async/chan)
                   :url (-> @routing/state* :route)
                   :method :post
                   :json-params (-> @data*
                                    (update-in [:valid_for_hours] int))}
                  http-client/request :chan <!
                  http-client/filter-success! :body)]
        (reset! data* res))))

(defn create-form-component [data*]
  [:div
   [:form.form
    {:on-submit (fn [e]
                  (.preventDefault e)
                  (submit data*))}
    [routing/hidden-state-component
     {:did-change #(reset! data* data-defaults)}]
    [forms/select-component
     data* [:valid_for_hours] {(* 1 1) "1 hour"
                               (* 1 24) "24 hours"
                               (* 3 24) "3 days"
                               (* 7 24) "7 days" }
     24
     :label [:span "Link valid for  "] ]
    [:div
     [:div.float-left
      [:button.btn.btn-warning
       {:on-click (fn [e]
                    (.preventDefault e)
                    (reset! data* data-defaults))}
       [icons/delete] " Reset"]]]
    [forms/submit-component
     :btn-classes [:btn-primary]
     :inner [:span [icons/add] " Create"]]
    ]])


(defn reset-path-url []
  (str (-> @state/global-state*
           :server-state :settings :external_base_url)
       (path :reset-password {}))

  )

(defn reset-full-url [token]
  (str (-> @state/global-state* :server-state :settings :external_base_url)
       (path :reset-password {} {:token token})))

(defn mail-link [data*]
  [:<>
   (when-let [email-address (:email @user-data*)]
     [:div
      [:div.d-flex.justify-content-center
       [:a.btn.btn-outline-success
        {:href (str "mailto:" email-address
                    "?subject=" (url/encode "Password Reset for Leihs")
                    "&body=" (url/encode (clojure.string/join
                                           "\n"
                                           [ (str "Click on " (reset-full-url (:token @data*)))
                                            "" "" "or visit "  " " (str "  " (reset-path-url))
                                            "" "and enter " " " (str "  " (:token @data*))
                                            "" "to reset you password for leihs. "
                                            ""
                                            "This token is valid until: "
                                            (str "  " (-> @data* :valid_until date-fns/parseISO str))])))}
        [icons/email] " Send the password reset link via e-mail"]]
      [:div.clearfix.mb-3]]
     )])

(defn show-component [data*]
  [:div
   [mail-link data*]
   [:div.alert.alert-success
    (let [url (reset-full-url (:token @data*))]
      [:div
       [:div
        [:p [:span "visit " ]]
        [:p.text-center
         {:style {:font-size "125%"}} [:span.code (reset-path-url)]]]
       [:div
        [:p [:span "enter token"]]
        [:p.text-center
         {:style {:font-size "150%"}} [:span.code (:token @data*)]]]
       [:hr]
       [:p "or scan "]
       [:div.d-flex.justify-content-center
        [qrcode-recat/QRCodeSVG #js{:value url :size 256}]]
       [:hr]
       [:div
        [:p "this token is valid until: "]
        [:p.text-center
         (-> @data* :valid_until date-fns/parseISO str)]]])]
   [:div.float-right
    [:button.btn.btn-primary
     {:on-click #(reset! data* data-defaults)}
     [:span [icons/delete] " Close "]]]
   [:div.clearfix.mb-2]
   ])

;for debugging
;(defonce data* (reagent/atom data-defaults))

(defn reset-link-no-possible-warning-component []
  [:div.alert.alert-warning {:role :alert}
   [:h3.alert-heading "Creating a Password Reset Link Is Not Possible"]
   [:p "To create and use a password reset link the folling must be sattisfied:"]
   [:ul
    [:li "The  " [:code "account_enabled"] " property must be set, and"]
    [:li "The  " [:code "password_sign_in_enabled"] " property must be set, and"]
    [:li "the account must have an " [:code "email"] " address."]]])

(defn main-component []
  (let [data* (reagent/atom data-defaults)]
    (fn []
      [:<>
       (when @state/debug?*
         [:pre (with-out-str (pprint @data*))]
         [:pre (with-out-str (pprint @user-data*))])
       (if-not @user-password-resetable?*
         [reset-link-no-possible-warning-component]
         (if (= (:mode @data*) :create)
           [create-form-component data*]
           [show-component data*]))])))

(defn page []
  [:div.user-password-reset
   [routing/hidden-state-component
    {:did-change #(core/clean-and-fetch)}]
   [breadcrumbs/nav-component
    (conj @breadcrumbs/left* [breadcrumbs/password-reset-li])[]]
   [:h1 "Password Reset Link for "
    [core/name-link-component]]
   [main-component]])
