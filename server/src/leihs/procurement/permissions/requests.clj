(ns leihs.procurement.permissions.requests
  (:require [leihs.procurement.permissions.user :as user-perms]
            [leihs.procurement.utils.sql :as sql]))

(defn apply-scope
  [tx sqlmap auth-entity]
  "If the user is admin or inspector then he can see all requests.
  Otherwise he sees only requests he is either viewer of their categories
  or their requester."
  (let [user-id (:user_id auth-entity)]
    (if (or (user-perms/admin? tx auth-entity)
            (user-perms/inspector? tx auth-entity))
      sqlmap
      (-> sqlmap
          (sql/merge-left-join :procurement_category_viewers
                               [:= :procurement_category_viewers.category_id
                                :procurement_requests.category_id])
          (sql/merge-where [:or
                            [:= :procurement_category_viewers.user_id user-id]
                            [:= :procurement_requests.user_id user-id]])))))
