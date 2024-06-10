(ns leihs.admin.paths
  (:refer-clojure :exclude [str keyword])
  (:require
   #?@(:clj
       [[logbug.catcher :as catcher]
        [logbug.debug :as debug]
        [logbug.thrown :as thrown]])
   [bidi.verbose :refer [branch leaf param]]
   [leihs.admin.resources.audits.paths :as audits]
   [leihs.admin.resources.inventory-pools.paths :as inventory-pools]
   [leihs.admin.resources.inventory.paths :as inventory]
   [leihs.admin.resources.settings.paths :as settings]
   [leihs.admin.resources.system.paths :as system]
   [leihs.core.paths]
   [taoensso.timbre :refer [error]]))

(def external-handlers
  #{:admin-audits-legacy
    :borrow
    :home
    :inventory-csv
    :inventory-excel
    :inventory-quick-csv
    :inventory-quick-excel
    :lending
    :procurement
    :status})

(def users-paths
  (branch "/users"
          (branch "/"
                  (leaf "" :users)
                  (branch "/"
                          (leaf "new" :user-create)
                          (leaf "choose" :users-choose)))
          (branch "/"
                  (param [#"[^/]+" :user-id])
                  (leaf "" :user)
                  (leaf "/password-reset" :user-password-reset)
                  (leaf "/delete" :user-delete)
                  (leaf "/edit" :user-edit)
                  (leaf "/inventory-pools/" :user-inventory-pools)
                  (branch "/api-tokens"
                          (branch "/"
                                  (leaf "" :user-api-tokens)
                                  (leaf "create" :user-api-tokens-create))
                          (branch "/"
                                  (param :api-token-id)
                                  (leaf "" :user-api-token)
                                  (leaf "delete" :user-api-token-delete)
                                  (leaf "edit" :user-api-token-edit)))
                  (branch "/transfer/"
                          (param [#"[^/]+" :target-user-uid])
                          (leaf "" :user-transfer-data)))))

(def groups-paths
  (branch "/groups"
          (branch "/"
                  (leaf "" :groups)
                  (leaf "create" :group-create))
          (branch "/"
                  (param :group-id)
                  (leaf "" :group)
                  (leaf "/delete" :group-delete)
                  (leaf "/edit" :group-edit)
                  (leaf "/inventory-pools-roles/" :group-inventory-pools-roles)
                  (branch "/users"
                          (leaf "/" :group-users)
                          (branch "/"
                                  (param :user-id)
                                  (leaf "" :group-user))))))

(def statistics-paths
  (branch "/statistics/"
          (leaf  "" :statistics)
          (leaf "contracts/" :statistics-contracts)
          (leaf "items/" :statistics-items)
          (leaf "models/" :statistics-models)
          (leaf "pools/" :statistics-pools)
          (leaf "users/" :statistics-users)))

(def suppliers-paths
  (branch "/suppliers"
          (branch "/"
                  (leaf "" :suppliers)
                  (leaf "create" :supplier-create))
          (branch "/"
                  (param :supplier-id)
                  (leaf "" :supplier)
                  (leaf "/delete" :supplier-delete)
                  (leaf "/edit" :supplier-edit)
                  (leaf "/items" :supplier-items))))

(def inventory-fields-paths
  (branch "/inventory-fields"
          (branch "/"
                  (leaf "" :inventory-fields)
                  (leaf "groups" :inventory-fields-groups)
                  (leaf "create" :inventory-field-create))
          (branch "/"
                  (param :inventory-field-id)
                  (leaf "" :inventory-field)
                  (leaf "/delete" :inventory-field-delete)
                  (leaf "/edit" :inventory-field-edit))))

(def buildings-paths
  (branch "/buildings"
          (branch "/"
                  (leaf "" :buildings)
                  (leaf "create" :building-create))
          (branch "/"
                  (param :building-id)
                  (leaf "" :building)
                  (leaf "/delete" :building-delete)
                  (leaf "/edit" :building-edit)
                  (leaf "/items" :building-items))))

(def rooms-paths
  (branch "/rooms"
          (branch "/"
                  (leaf "" :rooms)
                  (leaf "create" :room-create))
          (branch "/"
                  (param :room-id)
                  (leaf "" :room)
                  (leaf "/delete" :room-delete)
                  (leaf "/edit" :room-edit)
                  (leaf "/items" :room-items))))

(def mail-templates-paths
  (branch "/mail-templates"
          (branch "/"
                  (leaf "" :mail-templates)
                  (leaf "create" :mail-template-create))
          (branch "/"
                  (param :mail-template-id)
                  (leaf "" :mail-template)
                  (leaf "/delete" :mail-template-delete)
                  (leaf "/edit" :mail-template-edit)
                  (leaf "/items" :mail-template-items))))

(def paths
  (branch ""
          leihs.core.paths/core-paths
          (branch "/admin"
                  (leaf "/status" :status)
                  (leaf "/debug" :debug)
                  audits/paths
                  buildings-paths
                  groups-paths
                  inventory-fields-paths
                  inventory-pools/paths
                  inventory/paths
                  mail-templates-paths
                  rooms-paths
                  settings/paths
                  system/paths
                  users-paths
                  statistics-paths
                  suppliers-paths
                  (leaf "/audits" :admin-audits-legacy))))

(reset! leihs.core.paths/paths* paths)

(defn path [& args]
  (try
    (apply leihs.core.paths/path args)
    #?(:cljs
       (catch :default e
         (error e args)
         (throw e)))))

;(path :system-admins-direct-user {:user-id "foo"})
;(path :user-inventory-pools {:user-id "123"})
