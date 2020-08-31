(ns leihs.admin.shared.membership.groups.shared
  (:require
    [leihs.admin.resources.groups.shared :as groups-shared]))

(def default-query-params
  (merge groups-shared/default-query-params
         {:membership "member"}))
