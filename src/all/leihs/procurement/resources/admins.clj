(ns leihs.procurement.resources.admins
  (:require [clojure.java.jdbc :as jdbc]
            [leihs.procurement.utils.sql :as sql]))

(def admins-base-query
  (-> (sql/select :users.*)
      (sql/from :users)
      (sql/merge-where [:in
                        :users.id
                        (-> (sql/select :procurement_admins.user_id)
                            (sql/from :procurement_admins))])))

(defn get-admins [context _ _]
  (jdbc/query (-> context :request :tx)
              (sql/format admins-base-query)))
