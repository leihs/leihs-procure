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
    [leihs.admin.resources.delegation.front :as delegation]
    [leihs.admin.resources.delegation.users.front :as delegation-users]
    [leihs.admin.resources.delegations.front :as delegations]
    [leihs.admin.resources.group.front :as group]
    [leihs.admin.resources.group.users.front :as group-users]
    [leihs.admin.resources.groups.front :as groups]
    [leihs.admin.resources.home.front :as home]
    [leihs.admin.resources.inventory-pools.front :as inventory-pools]
    [leihs.admin.resources.inventory-pools.inventory-pool.front :as inventory-pool]
    [leihs.admin.resources.inventory-pools.inventory-pool.users.front :as inventory-pool-users]
    [leihs.admin.resources.inventory-pools.inventory-pool.users.user.front :as inventory-pool-user]
    [leihs.admin.resources.inventory-pools.inventory-pool.users.user.roles.front :as inventory-pool-user-roles]
    [leihs.admin.resources.inventory-pools.inventory-pool.users.user.suspension.front :as inventory-pool-user-suspension]
    [leihs.admin.resources.status.front :as status]
    [leihs.admin.resources.system.authentication-system.front :as authentication-system]
    [leihs.admin.resources.system.authentication-system.groups.front :as authentication-system-groups]
    [leihs.admin.resources.system.authentication-system.users.front :as authentication-system-users]
    [leihs.admin.resources.system.authentication-systems.front :as authentication-systems]
    [leihs.admin.resources.system.database.audits.front :as database-audits]
    [leihs.admin.resources.system.database.front :as database]
    [leihs.admin.resources.system.front :as system]
    [leihs.admin.resources.system.system-admins.direct-users.front :as system-admin-direct-users]
    [leihs.admin.resources.system.system-admins.front :as system-admins]
    [leihs.admin.resources.system.system-admins.groups.front :as system-admin-groups]
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
   :authentication-system #'authentication-system/show-page
   :authentication-system-add #'authentication-system/add-page
   :authentication-system-delete #'authentication-system/delete-page
   :authentication-system-edit #'authentication-system/edit-page
   :authentication-system-groups #'authentication-system-groups/page
   :authentication-system-user-edit #'authentication-system-users/edit-page
   :authentication-system-users #'authentication-system-users/index-page
   :authentication-systems #'authentication-systems/page
   :database #'database/page
   :database-audits #'database-audits/index-page
   :database-audits-before #'database-audits/before-page
   :debug #'leihs.admin.front.pages.debug/page
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
   :inventory-pool #'inventory-pool/show-page
   :inventory-pool-add #'inventory-pool/add-page
   :inventory-pool-delete #'inventory-pool/delete-page
   :inventory-pool-edit #'inventory-pool/edit-page
   :inventory-pools #'inventory-pools/page
   :inventory-pool-user #'inventory-pool-user/page
   :inventory-pool-user-roles #'inventory-pool-user-roles/page
   :inventory-pool-user-suspension #'inventory-pool-user-suspension/page
   :inventory-pool-users #'inventory-pool-users/index-page
   :status #'status/info-page
   :system #'system/page
   :system-admin-direct-users #'system-admin-direct-users/page
   :system-admin-groups #'system-admin-groups/page
   :system-admins #'system-admins/page
   :user #'user/show-page
   :user-delete #'user/delete-page
   :user-edit #'user/edit-page
   :user-inventory-pools-roles #'user/inventory-pools-roles-page
   :user-new #'user/new-page
   :users #'users/page
   })

(defn init []
  (routing/init paths resolve-table paths/external-handlers))
