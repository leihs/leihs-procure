(ns leihs.admin.resources.users.shared)

(def default-fields 
  #{
    :email 
    :firstname 
    :id 
    :img32_url 
    :lastname 
    :login 
    :org_id
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
    :firstname
    :id
    :img256_url
    :img32_url
    :img_digest
    :is_admin
    :lastname
    :login
    :org_id
    :password_sign_in_enabled
    :phone
    :updated_at
    :url
    :zip
    })

(def default-query-parameters {:is_admin nil
                               :role "any"
                               :page 1
                               :per-page 12
                               :term ""
                               :type "any" })

(defn normalized-query-parameters [query-params]
  (merge default-query-parameters query-params))
