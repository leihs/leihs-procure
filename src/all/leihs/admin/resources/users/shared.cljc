(ns leihs.admin.resources.users.shared
  )

(def default-query-parameters {:is_admin nil
                               :role "any"
                               :page 1
                               :per-page 12
                               :term ""
                               :type "any" })

(defn normalized-query-parameters [query-params]
  (merge default-query-parameters query-params))
