(ns leihs.admin.resources.users.user.password-reset.main
  (:require
   ["date-fns" :as date-fns]
   [cljs.core.async :as async :refer [<! go]]
   [clojure.string :refer [join]]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.common.icons :as icons]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.users.user.core :as core :refer [user-data*]]
   [leihs.admin.state :as state]
   [leihs.core.core :refer [presence]]
   [leihs.core.routing.front :as routing]
   [leihs.core.url.core :as url]
   [qrcode.react :as qrcode-recat]
   [react-bootstrap :as react-bootstrap :refer [Button Modal InputGroup FormControl]]
   [reagent.core :as reagent :refer [reaction]]))

(defonce data* (reagent/atom {}))

(defn format-date [date]
  (-> date date-fns/parseISO
      (date-fns/format "EEEE dd'.' MMMM yyyy '-' HH:mm") str))

(def user-password-resetable?*
  (reaction
   (and (-> @user-data* :account_enabled)
        (-> @user-data* :password_sign_in_enabled)
        (or (-> @user-data* :email presence)
            (-> @user-data* :login presence)))))

(defn submit [& {:keys [valid_for]
                 :or {valid_for 24}}]
  (go (let [res (some->
                 {:chan (async/chan)
                  :url (-> @routing/state* :route (str "/password-reset"))
                  :method :post
                  :json-params {:valid_for_hours valid_for}}
                 http-client/request :chan <!
                 http-client/filter-success! :body)]
        (reset! data* res))))

(defn reset-path-url []
  (str (-> @state/global-state*
           :server-state :settings :external_base_url)
       (path :reset-password {})))

(defn reset-full-url [token]
  (str (-> @state/global-state* :server-state :settings :external_base_url)
       (path :reset-password {} {:token token})))

(defn mail-link-button []
  (when-let [email-address (:email @user-data*)]
    [:> Button
     {:variant "success"
      :disabled (not (@data* :token))
      :href (str "mailto:" email-address
                 "?subject=" (url/encode "Password Reset for Leihs")
                 "&body=" (url/encode (join
                                       "\n"
                                       [(str "Click on " (reset-full-url (:token @data*)))
                                        "" "" "or visit "  " " (str "  " (reset-path-url))
                                        "" "and enter " " " (str "  " (:token @data*))
                                        "" "to reset you password for leihs. "
                                        ""
                                        "This token is valid until: "
                                        (str "  " (when (@data* :valid_until)
                                                    (-> @data* :valid_until format-date)))])))}
     [icons/email] " Send per Mail"]))

(defn show-component []
  (let [url (reset-full-url (:token @data*))]
    [:<>
     [:> InputGroup {:className "mb-3"}
      [:> FormControl {:value (or (reset-path-url) "")
                       :disabled true}]
      [:> InputGroup.Append
       [:> Button
        {:variant "secondary"
         :href (reset-path-url)
         :target "_blank"}
        "Visit"]]]
     [:> InputGroup {:className "mb-5"}
      [:> InputGroup.Prepend
       [:> InputGroup.Text "Reset Token"]]
      [:> FormControl {:id "reset-token"
                       :value (:token (or @data* ""))
                       :disabled true}]
      [:> InputGroup.Append
       [:> Button {:variant "secondary"
                   :onClick #(js/navigator.clipboard.writeText (:token @data*))}
        "Copy"]]]
     [:div.d-flex.justify-content-center
      [qrcode-recat/QRCodeSVG #js{:value url :size 256}]]
     [:hr]
     [:div
      [:p.font-weight-bold "Valid until"]
      [:p
       (when (@data* :valid_until)
         (-> @data* :valid_until format-date))]]]))

(defn reset-link-no-possible-warning-component []
  [:div.alert.alert-warning {:role :alert}
   [:h3.alert-heading "Creating a Password Reset Link Is Not Possible"]
   [:p "To create and use a password reset link the folling must be sattisfied:"]
   [:ul
    [:li "The  " [:code "account_enabled"] " property must be set, and"]
    [:li "The  " [:code "password_sign_in_enabled"] " property must be set, and"]
    [:li "the account must have an " [:code "email"] " address."]]])

(defn dialog [& {:keys [show onHide]
                 :or {show false}}]
  [:> Modal {:size "md"
             :centered true
             :scrollable true
             :show show}
   [:> Modal.Header {:closeButton true
                     :onHide onHide}
    [:> Modal.Title "Reset Password"]]
   [:> Modal.Body
    (if-not @user-password-resetable?*
      [reset-link-no-possible-warning-component]
      (when (seq @data*)
        [show-component data*]))]
   [:> Modal.Footer
    [:> Button {:variant "secondary"
                :onClick onHide}
     "Cancel"]
    [mail-link-button data*]]])

(defn page []
  [:div.user-password-reset
   [routing/hidden-state-component
    {:did-change #(core/clean-and-fetch)}]
   [:h1 "Password Reset Link for "
    [core/name-link-component]]])

