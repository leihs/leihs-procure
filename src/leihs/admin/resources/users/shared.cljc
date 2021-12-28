(ns leihs.admin.resources.users.shared
  (:require
    [leihs.admin.constants :as defaults]))

(def default-fields
  #{:account_enabled
    :admin_protected
    :email
    :firstname
    :id
    :img32_url
    :lastname
    :login
    :org_id
    :organization})

(def available-fields
  #{:account_disabled_at
    :account_enabled
    :address
    :admin_protected
    :badge_id
    :city
    :country
    :created_at
    :email
    :extended_info
    :firstname
    :id
    :img256_url
    :img32_url
    :img_digest
    :is_admin
    :is_system_admin
    :last_sign_in_at
    :lastname
    :login
    :org_id
    :organization
    :password_sign_in_enabled
    :phone
    :secondary_email
    :system_admin_protected
    :updated_at
    :url
    :zip})

(def default-query-params
  {:account_enabled "yes"
   :is_admin nil
   :org_id nil
   :page 1
   :per-page defaults/PER-PAGE
   :term "" })

(defn normalized-query-parameters [query-params]
  (merge default-query-params query-params))
