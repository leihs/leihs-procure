(ns leihs.admin.routes
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    )
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.routing.front :as routing]
    [leihs.core.url.query-params :as query-params]

    [leihs.admin.front.pages.debug]
    [leihs.admin.paths :as paths :refer [path paths]]
    [leihs.admin.resources.admin.front :as admin]
    [leihs.admin.resources.authentication-system.front :as authentication-system]
    [leihs.admin.resources.authentication-system.users.front :as authentication-system-users]
    [leihs.admin.resources.authentication-systems.front :as authentication-systems]
    [leihs.admin.resources.delegation.front :as delegation]
    [leihs.admin.resources.delegation.users.front :as delegation-users]
    [leihs.admin.resources.delegations.front :as delegations]
    [leihs.admin.resources.group.front :as group]
    [leihs.admin.resources.group.users.front :as group-users]
    [leihs.admin.resources.groups.front :as groups]
    [leihs.admin.resources.home.front :as home]
    [leihs.admin.resources.sign-in.front :as sign-in]
    [leihs.admin.resources.status.front :as status]
    [leihs.admin.resources.user.front :as user]
    [leihs.admin.resources.users.front :as users]

    [accountant.core :as accountant]
    [bidi.bidi :as bidi]
    [cljsjs.js-yaml]
    [clojure.pprint :refer [pprint]]
    [reagent.core :as reagent]
    ))

(def resolve-table
  {
   :admin #'admin/page
   :debug #'leihs.admin.front.pages.debug/page
   :authentication-system #'authentication-system/show-page
   :authentication-system-add #'authentication-system/add-page
   :authentication-system-delete #'authentication-system/delete-page
   :authentication-system-edit #'authentication-system/edit-page
   :authentication-system-user-edit #'authentication-system-users/edit-page
   :authentication-system-users #'authentication-system-users/index-page
   :authentication-systems #'authentication-systems/page
   :delegation #'delegation/show-page
   :delegation-add #'delegation/new-page
   :delegation-add-choose-responsible-user #'delegation/choose-responsible-user-page
   :delegation-delete #'delegation/delete-page
   :delegation-edit #'delegation/edit-page
   :delegation-edit-choose-responsible-user #'delegation/choose-responsible-user-page
   :delegation-users #'delegation-users/index-page
   :delegations #'delegations/page
   :group #'group/show-page
   :group-add #'group/add-page
   :group-delete #'group/delete-page
   :group-edit #'group/edit-page
   :group-users #'group-users/index-page
   :groups #'groups/page
   :home #'home/page
   :sign-in #'sign-in/page
   :status #'status/info-page
   :user #'user/show-page
   :user-delete #'user/delete-page
   :user-edit #'user/edit-page
   :user-inventory-pools-roles #'user/inventory-pools-roles-page
   :user-new #'user/new-page
   :user-password #'user/password-page
   :users #'users/page
   })

(defn init []
  (routing/init paths resolve-table paths/external-handlers))
