(ns leihs.admin.shared.membership.users.shared
  (:require
    [leihs.admin.resources.users.shared :as users-shared]))

(def default-query-params
  (merge users-shared/default-query-params
         {:membership "member"}))
