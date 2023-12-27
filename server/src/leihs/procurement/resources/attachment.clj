(ns leihs.procurement.resources.attachment
  (:require

    [honey.sql :refer [format] :rename {format sql-format}]
    [leihs.core.db :as db]
    [next.jdbc :as jdbc]
    [honey.sql.helpers :as sql]

    [leihs.core.utils :refer [my-cast]]

    [taoensso.timbre :refer [debug info warn error spy]]

    ;[clojure.java.jdbc :as jdbc]
    ;        [leihs.procurement.utils.sql :as sql]

    [compojure.core :as cpj]
    [leihs.procurement.paths :refer [path]]
    )
  (:import java.util.Base64))

(def attachment-base-query
  (-> (sql/select :procurement_attachments.*)
      (sql/from :procurement_attachments)))

(defn attachment-query
  [id]
  (-> attachment-base-query
      (sql/where [:= :procurement_attachments.id id])))

(defn attachment
  [{tx :tx-next, {attachment-id :attachment-id} :route-params}]
  (if-let [a (->> attachment-id
                  attachment-query
                  sql-format
                  (jdbc/execute-one! tx)
                  )]
    (->> a
         :content
         (.decode (Base64/getMimeDecoder))
         (hash-map :body)
         (merge
           {:headers {"Content-Type" (:content_type a),
                      "Content-Transfer-Encoding" "binary",
                      "Content-Disposition"
                      (str "inline; " "filename=\"" (:filename a) "\"")}}))
    {:status 404}))

(def attachment-path (path :attachment {:attachment-id ":attachment-id"}))

(def routes (cpj/routes (cpj/GET attachment-path [] #'attachment)))

(defn create!
  [tx data]
  (jdbc/execute! tx (-> (sql/insert-into :procurement_attachments)
                        (sql/values [(my-cast data)])
                        sql-format)))

