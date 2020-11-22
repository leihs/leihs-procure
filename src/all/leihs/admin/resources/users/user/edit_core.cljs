(ns leihs.admin.resources.users.user.edit-core
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.requests.core :as requests]
    [leihs.core.routing.front :as routing]
    [leihs.core.user.front :as current-user]

    [leihs.admin.common.form-components :refer [checkbox-component input-component]]
    [leihs.admin.state :as state]

    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.users.user.core :as core :refer [user-id*]]

    [accountant.core :as accountant]
    [cljs.core.async :as async]
    [cljs.core.async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [cljsjs.jimp]
    [cljsjs.moment]
    [clojure.contrib.inflect :refer [pluralize-noun]]
    [reagent.core :as reagent]

    [taoensso.timbre :as logging]
    ))


(defonce data* (reagent/atom nil))

(defn json-component
  [kw & {:keys [label hint]
         :or {label kw
              hint nil}}]
  (let [is-valid* (reaction
                    (try
                      (.parse js/JSON (get @data* kw))
                      true
                      (catch :default _ false)))]
    [:div.form-group
     [:label {:for kw}
      (if (= label kw)
        [:strong  label]
        [:span [:strong  label] [:small " (" [:span.text-monospace kw] ")"]])]
     [:textarea.form-control
      {:id kw
       :class (if @is-valid* "is-valid" "is-invalid")
       :auto-complete :off
       :value (or (kw @data*) "")
       :on-change #(swap! data* assoc kw (-> % .-target .-value presence))
       :tab-index 100
       :disabled false}]
     (when hint [:small hint])]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn essentials-form-component []
  [:div.essential-fields
    [:h3 "Essential Fields"]
    [:div.form-row
     [:div.col-md-5 [input-component data* [:email]
                     :type :email
                     :label "Email-address"
                     :validator (fn [value]
                                  (or false))
                     :hint [:span "A real email-address is essential for many processes inside leihs. "
                            "Each value must be " [:strong " unique accross all users "] " and  "
                            [:strong " must contain a " [:span.text-monospace "@"]] " sign."
                            " This field is not mandatory: "
                            [:strong "do not fill in made up email-addresses! "]
                            "Consider to use the " [:strong.text-monospace "login"] " field instead."
                            ]]]
     [:div.col-md-3 [input-component data* [:firstname]
                     :label "First name"]]
     [:div.col-md-4 [input-component data* [:lastname]
                     :label "Last name"]]]])

(defn personal-and-contact-form-component []
  [:div
   [:h3 "Personal and Contact Information "]
   [:div.form-row
    [:div.col-md
     [input-component data* [:phone]
      :type :phone :label "Phone number"]
     [input-component data* [:url]
      :label "URL"]]
    [:div.col-md [input-component data* [:secondary_email]
                  :label "Secondary email-address"
                  :hint [:span
                         "The secondary email-address is generally used as a fallback. "
                         "There is no unique constraint on the secondary so it can also be "
                         "used in cases where the same person has multiple accounts. "
                         "The secondary email address can not be used to sign in. "]]]]
   [:div.form-row
    [:div.col-md-5 [input-component data* [:address]
                    :label "Street address"]]
    [:div.col-md-3 [input-component data* [:zip]
                    :label "Zip code"]]
    [:div.col-md-4 [input-component data* [:country]
                    :label "Country"]]]])

(defn account-settings-form-component []
  [:div.account-settings
   [:h3 "Account Settings "]
   [:div.form-row
    [:div.col-md-3
     [checkbox-component data* [:account_enabled]
      :label "Enabled"
      :hint [:span  "A disabled account prevents sign in. "
             "Generally used if a user leaves the organization but the account can not be deleted. "]]]
    [:div.col-md-3
     [checkbox-component data* [:password_sign_in_enabled]
      :label "Password sign-in"
      :hint [:span "Often generally disabled when leihs is connected to an external authentication system."]]]
    [:div.col-md-3
     [checkbox-component data* [:protected]
      :disabled (not @current-user/admin?*)
      :label "Admin protected"
      :hint [:span "A protected account can only be modifed by admins and in particular not by inventory-pool staff. "
             "This is often set for accounts which are automatically managed via the API. "  ]]]
    [:div.col-md-3
     [checkbox-component data* [:is_admin]
      :disabled (not @current-user/admin?*)
      :label "Leihs admin"
      :hint "Marks this account to be a leihs admin or not."]]]

   [:div
    [:h3  "Other Fields "]
    [:div.form-row
     [:div.col-md
      [input-component data* [:login]
       :label "Login"
       :hint [:span "The login can be used alternative to the primary email-address to sign in. "
              "Each value must be " [:strong " unique accross all users "] " and  "
              [:strong " may not contain a " [:span.text-monospace "@"]] " sign."]]]
     [:div.col-md
      [input-component data* [:org_id]
       :label "Organizational ID"
       :hint [:span "This field is often used by automatically manged imports to identify users, "
              "in particular when the email-address is not guarantted to be stable (reuse). "
              "Each value must be " [:strong " unique accross all users."]]]]
     [:div.col-md
      [input-component data* [:badge_id]
       :label "Badge ID"
       :hint [:span "This value is meant to be used during the hand out or take back in conjunction "
              "with external machinery such as barcode or RFID scanners." ]]]]

    [:div
     [json-component :extended_info
      :label "Extended info"
      :hint [:span "This field can hold any structured data in JSON format."]]]]])

(defn debug-component []
  (when (:debug @state/global-state*)
    [:div
    [:div.data
     [:h3 "data*"]
     [:pre (with-out-str (pprint @data*))]]
    [:div
     [:h3 "@current-user/admin?*"]
     [:pre (with-out-str (pprint @current-user/admin?*))]]
    ]))
