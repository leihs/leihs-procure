(ns leihs.procurement.resources.main_categories
  (:require [clojure.java.jdbc :as jdbc]
            [leihs.procurement.utils.sql :as sql]))

(def main-categories-base-query
  (-> (sql/select :procurement_main_categories.*)
      (sql/from :procurement_main_categories)))

(defn get-main-categories [context _]
  (jdbc/query (-> context :request :tx)
              (sql/format main-categories-base-query)))
