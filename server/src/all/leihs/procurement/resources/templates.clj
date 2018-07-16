(ns leihs.procurement.resources.templates
  (:require [clojure.java.jdbc :as jdbc]
            [leihs.procurement.authorization :as authorization]
            [leihs.procurement.permissions.user :as user-perms]
            [leihs.procurement.utils.sql :as sql]))

(def templates-base-query
  (-> (sql/select :procurement_templates.*)
      (sql/from :procurement_templates)))

(defn get-templates
  [context _ _]
  (jdbc/query (-> context
                  :request
                  :tx)
              (sql/format templates-base-query)))

; (defn insert-templates!
;   [tx ts]
;   (jdbc/execute! tx
;                  (-> (sql/insert-into :procurement_templates)
;                      (sql/values [ts])
;                      sql/format)))

(defn insert-template!
  [tx tmpl]
  (jdbc/execute! tx
                 (-> (sql/insert-into :procurement_templates)
                     (sql/values [tmpl])
                     sql/format)))

(defn delete-templates!
  [tx]
  (jdbc/execute! tx
                 (-> (sql/delete-from :procurement_templates)
                     sql/format)))

(defn update-templates!
  [context args _]
  (let [rrequest (:request context)
        tx (:tx rrequest)
        auth-entity (:authenticated-entity rrequest)
        input-data (:input_data args)]
    (delete-templates! tx)
    (doseq [tmpl input-data]
      (authorization/authorize-and-apply
        #(insert-template! tx tmpl)
        :if-only
        #(user-perms/inspector? tx auth-entity (:category_id tmpl))))))
