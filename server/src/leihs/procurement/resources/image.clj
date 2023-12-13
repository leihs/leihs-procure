(ns leihs.procurement.resources.image
  (:require [cheshire.core :refer [generate-string] :rename
             {generate-string to-json}]

    ;[clojure.java.jdbc :as jdbc]
    ;        [leihs.procurement.utils.sql :as sql]

            [taoensso.timbre :refer [debug info warn error spy]]



            [honey.sql :refer [format] :rename {format sql-format}]
            [leihs.core.db :as db]
            [next.jdbc :as jdbc]
            [honey.sql.helpers :as sql]

            [compojure.core :as cpj]
            [leihs.procurement.paths :refer [path]]
            [leihs.procurement.resources.upload :as upload]
            )
  (:import java.util.Base64))

(def image-base-query
  (-> (sql/select :procurement_images.*)
      (sql/from :procurement_images)))

(defn image-query
  [id]
  (-> image-base-query
      (sql/where [:= :procurement_images.id [:cast id :uuid]])))

(defn image-query-for-main-category
  [id]
  (-> image-base-query
      (sql/where [:= :procurement_images.main_category_id [:cast id :uuid]])))

(defn image
  [{tx :tx-next, {image-id :image-id} :route-params}]
  (if-let [i (->> image-id
                  image-query
                  sql-format
                  (jdbc/execute-one! tx)
                  )]
    (->> i
         :content
         (.decode (Base64/getMimeDecoder))
         (hash-map :body)
         (merge {:headers {"Content-Type" (:content_type i),
                           "Content-Transfer-Encoding" "binary"}}))
    {:status 404}))

(defn cast-to-json [comment] [:cast comment :json])



(defn my-cast [data]
  (println ">o> no / 22 / my-cast /debug " data)


  (let [
        data (if (contains? data :id)
               (assoc data :id [[:cast (:id data) :uuid]])
               data
               )

        data (if (contains? data :category_id)
               (assoc data :category_id [[:cast (:category_id data) :uuid]])
               data
               )
        data (if (contains? data :template_id)
               (assoc data :template_id [[:cast (:template_id data) :uuid]])
               data
               )

        data (if (contains? data :room_id)
               (assoc data :room_id [[:cast (:room_id data) :uuid]])
               data
               )

        data (if (contains? data :order_status)
               (assoc data :order_status [[:cast (:order_status data) :order_status_enum]])
               data
               )

        data (if (contains? data :budget_period_id)
               (assoc data :budget_period_id [[:cast (:budget_period_id data) :uuid]])
               data
               )

        data (if (contains? data :user_id)
               (assoc data :user_id [[:cast (:user_id data) :uuid]])
               data
               )

        data (if (contains? data :main_category_id)
               (assoc data :main_category_id [[:cast (:main_category_id data) :uuid]])
               data
               )
        ]
    (spy data)
    )
  )

(defn insert!
  [tx data]
  (jdbc/execute! tx (-> (sql/insert-into :procurement_images)
                        (sql/values [(my-cast data)])
                        sql-format)))

(defn create-for-main-category-id-and-upload!
  [tx mc-id upload]
  (let [{u-id :id} upload
        p (println ">o> u-id" u-id)

        u-row (upload/get-by-id tx (spy u-id))
        p (println ">o> u-row" u-row)

        md (-> u-row
               :metadata
               to-json
               cast-to-json)]
    (insert! tx
             (-> u-row
                 (dissoc :id)
                 (dissoc :created_at)
                 (assoc :metadata md)
                 (assoc :main_category_id mc-id)))
    (upload/delete! tx u-id)))

(def image-path (path :image {:image-id ":image-id"}))

(def routes (cpj/routes (cpj/GET image-path [] #'image)))
