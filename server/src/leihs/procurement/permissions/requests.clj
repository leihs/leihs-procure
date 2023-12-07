(ns leihs.procurement.permissions.requests
  (:require [leihs.procurement.permissions.user :as user-perms]
            ;[leihs.procurement.utils.sql :as sql]

          [honey.sql :refer [format] :rename {format sql-format}]
          [leihs.core.db :as db]
          [next.jdbc :as jdbc]
          [honey.sql.helpers :as sql]

                [taoensso.timbre :refer [debug info warn error spy]]


    ))

(defn apply-scope
  [tx sqlmap auth-entity]
  "If the user is admin or inspector then he can see all requests.
  Otherwise he sees only requests he is either viewer of their categories
  or their requester."
  (let [user-id (:user_id auth-entity)
        p (println ">o> apply-scope" user-id)
        ]
    (if (or (user-perms/admin? tx auth-entity)
            (user-perms/inspector? tx auth-entity))
      sqlmap                                          ;; TODO: do sama
      (-> sqlmap
          (sql/left-join :procurement_category_viewers
                               [:= :procurement_category_viewers.category_id
                                :procurement_requests.category_id])
          (sql/where [:or
                            [:= :procurement_category_viewers.user_id [:cast user-id :uuid]]
                            [:= :procurement_requests.user_id [:cast user-id :uuid]]]))


      )))
