(ns leihs.procurement.resources.current-user
  (:require [leihs.core.remote-navbar.shared :refer [navbar-props]]
            [leihs.core.json :refer [to-json]]
            [leihs.procurement.resources [saved-filters :as saved-filters]
             [user :as user]]))

(defn get-current-user
  [{request :request} _ _]
  (let [tx (:tx-next request)
        user-id (-> request
                    :authenticated-entity
                    :user_id)

        p (println ">>>1" user-id)

        user (user/get-user-by-id tx user-id)
        p (println ">>>2" user)
        saved-filters (saved-filters/get-saved-filters-by-user-id tx user-id)
        p (println ">>>3" saved-filters)

        ] ;;FIXME
    {:user user,
     :saved_filters (:filter saved-filters),
     :navbarProps (-> request
                      (navbar-props {:procure false})
                      to-json)}))
