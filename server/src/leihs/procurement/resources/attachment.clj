(ns leihs.procurement.resources.attachment
  (:require

    [honey.sql :refer [format] :rename {format sql-format}]
    [leihs.core.db :as db]
    [next.jdbc :as jdbc]
    [honey.sql.helpers :as sql]

    [leihs.procurement.utils.helpers :refer [my-cast]]

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


;(defn my-cast [data]
;  (println ">o> no / 22 / my-cast /debug " data)
;
;
;  (let [
;        data (if (contains? data :id)
;               (assoc data :id [[:cast (:id data) :uuid]])
;               data
;               )
;
;        data (if (contains? data :category_id)
;               (assoc data :category_id [[:cast (:category_id data) :uuid]])
;               data
;               )
;        data (if (contains? data :template_id)
;               (assoc data :template_id [[:cast (:template_id data) :uuid]])
;               data
;               )
;
;        data (if (contains? data :room_id)
;               (assoc data :room_id [[:cast (:room_id data) :uuid]])
;               data
;               )
;
;        data (if (contains? data :order_status)
;               (assoc data :order_status [[:cast (:order_status data) :order_status_enum]])
;               data
;               )
;
;        data (if (contains? data :budget_period_id)
;               (assoc data :budget_period_id [[:cast (:budget_period_id data) :uuid]])
;               data
;               )
;
;        data (if (contains? data :user_id)
;               (assoc data :user_id [[:cast (:user_id data) :uuid]])
;               data
;               )
;
;        data (if (contains? data :request_id)
;               (assoc data :request_id [[:cast (:request_id data) :uuid]])
;               data
;               )
;
;        data (if (contains? data :metadata)
;               (do
;                 (println ">o> upload::metadata")
;                 (assoc data :metadata [[:cast (:metadata data) :jsonb]]) ;; works as local-test
;                 ;(assoc data :metadata [[:cast (:metadata data) :json]])
;                 ;(assoc data :metadata [[:cast (:metadata data) :text]]))
;                 )
;               data
;               )
;
;        ;[[:cast (to-name-and-lower-case a) :order_status_enum]]
;
;        ]
;    (spy data)
;    )
;
;  )

(defn create!
  [tx data]
  (jdbc/execute! tx (-> (sql/insert-into :procurement_attachments)
                        (sql/values [(my-cast data)])
                        sql-format)))

