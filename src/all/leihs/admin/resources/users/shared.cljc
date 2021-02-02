(ns leihs.admin.resources.users.shared
  (:require
    [leihs.admin.defaults :as defaults]))

(def default-fields
  #{
    :account_enabled
    :admin_protected
    :email
    :firstname
    :id
    :img32_url
    :lastname
    :login
    :org_id
    :organization
    })

(def available-fields
  #{
    :account_enabled
    :address
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
    :lastname
    :login
    :org_id
    :organization
    :password_sign_in_enabled
    :phone
    :secondary_email
    :updated_at
    :url
    :zip
    })

(def default-query-params
  {:account_enabled nil
   :is_admin nil
   :org_id nil
   :page 1
   :per-page defaults/PER-PAGE
   :term "" })

(defn normalized-query-parameters [query-params]
  (merge default-query-params query-params))
