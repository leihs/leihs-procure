(ns leihs.admin.routes
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    )
  (:require
    [leihs.admin.front.pages.debug]
    [leihs.admin.front.requests.pages.request]
    [leihs.admin.front.requests.pages.requests]
    [leihs.admin.front.state :refer [routing-state*]]
    [leihs.admin.paths :as paths :refer [path paths]]
    [leihs.admin.resources.admin.front :as admin]
    [leihs.admin.resources.api-token.front :as api-token]
    [leihs.admin.resources.api-tokens.front :as api-tokens]
    [leihs.admin.resources.auth.front :as auth]
    [leihs.admin.resources.delegation.front :as delegation]
    [leihs.admin.resources.delegation.users.front :as delegation-users]
    [leihs.admin.resources.delegations.front :as delegations]
    [leihs.admin.resources.home.front :as home]
    [leihs.admin.resources.initial-admin.core :as initial-admin]
    [leihs.admin.resources.user.front :as user]
    [leihs.admin.resources.users.front :as users]
    [leihs.admin.utils.core :refer [keyword str presence]]
    [leihs.admin.utils.url.query-params :refer [decode-query-params]]

    [accountant.core :as accountant]
    [bidi.bidi :as bidi]
    [cljsjs.js-yaml]
    [clojure.pprint :refer [pprint]]
    [reagent.core :as reagent]
    ))

(def resolve-table
  {
   :admin #'admin/page
   :api-token #'api-token/show-page
   :api-token-delete #'api-token/delete-page
   :api-token-edit #'api-token/edit-page
   :api-token-new #'api-token/new-page
   :api-tokens #'api-tokens/page
   :auth #'auth/auth-page
   :auth-password-sign-in #'auth/password-sign-in-page
   :debug #'leihs.admin.front.pages.debug/page
   :delegation #'delegation/show-page
   :delegation-delete #'delegation/delete-page
   :delegation-edit #'delegation/edit-page
   :delegation-edit-choose-responsible-user #'delegation/choose-responsible-user-page
   :delegation-add #'delegation/new-page
   :delegation-users #'delegation-users/index-page
   :delegation-add-choose-responsible-user #'delegation/choose-responsible-user-page
   :delegations #'delegations/page
   :initial-admin #'initial-admin/page
   :home #'home/page
   :request #'leihs.admin.front.requests.pages.request/page
   :requests #'leihs.admin.front.requests.pages.requests/page
   :user #'user/show-page
   :user-delete #'user/delete-page
   :user-edit #'user/edit-page
   :user-new #'user/new-page
   :users #'users/page
   })

(defn resolve-page [k]
  (get resolve-table k nil))

(defn match-path [path]
  (bidi/match-route paths path))

(defn init-navigation []
  (accountant/configure-navigation!
    {:nav-handler (fn [path]
                    (let [{route-params :route-params
                           handler-key :handler} (match-path path)
                          location-href (-> js/window .-location .-href)
                          location-url (goog.Uri. location-href)]
                      (swap! routing-state* assoc
                             :route-params route-params
                             :handler-key handler-key
                             :page (resolve-page handler-key)
                             :url location-href
                             :path (.getPath location-url)
                             :query-params (-> location-url .getQuery decode-query-params))
                      (js/console.log (with-out-str (pprint [handler-key route-params])))
                      ))
     :path-exists? (fn [path]
                     (js/console.log (with-out-str (pprint (match-path path))))
                     (boolean (when-let [handler-key (:handler (match-path path))]
                                (when-not (handler-key paths/external-handlers)
                                  handler-key))))}))

(defn init []
  (init-navigation)
  (accountant/dispatch-current!))
