(ns leihs.admin.resources.inventory-pools.inventory-pool.users.user.suspension.main
  (:require
   [leihs.admin.resources.inventory-pools.inventory-pool.suspension.core :as suspension]))

;;; routes ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def routes
  (fn [request]
    (case (:request-method request)
      :delete (suspension/delete-suspension request)
      :get (suspension/get-suspension request)
      :put (suspension/set-suspension request))))

;#### debug ###################################################################

;(debug/wrap-with-log-debug #'filter-by-access-right)
;(debug/wrap-with-log-debug #'users-formated-query)
;(debug/debug-ns *ns*)
