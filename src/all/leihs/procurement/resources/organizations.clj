(ns leihs.procurement.resources.organizations
  (:require [clojure.java.jdbc :as jdbc]
            [leihs.procurement.utils.sql :as sql]))

(def organizations-base-query
  (-> (sql/select :procurement_organizations.*)
      (sql/from :procurement_organizations)))

(defn get-organizations [context _ _]
  (jdbc/query (-> context :request :tx)
              (sql/format organizations-base-query)))
