(ns leihs.admin.resources.buildings.building.main
  (:require
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.admin.resources.buildings.shared :as shared]
   [leihs.core.uuid :refer [uuid]]
   [next.jdbc :as jdbc]
   [next.jdbc.sql :refer [query update!] :rename {query jdbc-query,
                                                  update! jdbc-update!}]))

;;; data keys ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def fields #{:name :code})

;;; building ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn building-query [building-id]
  (-> (sql/select :id :name :code shared/is-general-select-expr)
      (sql/from :buildings)
      (sql/where [:= :id building-id])))

(defn building [tx-next building-id]
  (-> building-id
      uuid
      building-query
      sql-format
      (->> (jdbc-query tx-next))
      first))

(defn get-building
  [{tx-next :tx-next {building-id :building-id} :route-params}]
  {:body (building tx-next building-id)})

;;; delete group ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn delete-building
  [{tx-next :tx-next {building-id :building-id} :route-params}]
  (assert building-id)
  (if (= (uuid building-id)
         (:id (jdbc/execute-one! tx-next
                                 ["DELETE FROM buildings WHERE id = ?" (uuid building-id)]
                                 {:return-keys true})))
    {:status 204}
    {:status 404 :body "Delete building failed without error."}))

;;; update building ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn patch-building
  [{{building-id :building-id} :route-params tx-next :tx-next data :body :as request}]
  (when (->> ["SELECT true AS exists FROM buildings WHERE id = ?" (uuid building-id)]
             (jdbc-query tx-next)
             first :exists)
    (jdbc-update! tx-next :buildings
                  (select-keys data fields)
                  ["id = ?" (uuid building-id)])
    {:status 204}))

;;; routes and paths ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn routes [request]
  (case (:request-method request)
    :get (get-building request)
    :patch (patch-building request)
    :delete (delete-building request)))

;#### debug ###################################################################

;(debug/wrap-with-log-debug #'data-url-img->buffered-image)
;(debug/wrap-with-log-debug #'buffered-image->data-url-img)
;(debug/wrap-with-log-debug #'resized-img)

;(debug/debug-ns *ns*)
