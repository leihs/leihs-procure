(ns leihs.admin.resources.suppliers.supplier.main
  (:refer-clojure :exclude [str keyword])
  (:require
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.core.uuid :refer [uuid]]
   [next.jdbc :as jdbc]
   [next.jdbc.sql :refer [insert! query update!] :rename {query jdbc-query,
                                                          insert! jdbc-insert!
                                                          update! jdbc-update!}]))

;;; data keys ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def fields #{:name :note})

;;; supplier ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn supplier-query [supplier-id]
  (-> (sql/select :id :name :note)
      (sql/from :suppliers)
      (sql/where [:= :id supplier-id])))

(defn supplier [tx supplier-id]
  (-> supplier-id
      uuid
      supplier-query
      sql-format
      (->> (jdbc-query tx))
      first))

(defn get-supplier
  [{tx :tx {supplier-id :supplier-id} :route-params}]
  {:body (supplier tx supplier-id)})

;;; delete group ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn delete-supplier
  [{tx :tx {supplier-id :supplier-id} :route-params}]
  (assert supplier-id)
  (if (= (uuid supplier-id)
         (:id (jdbc/execute-one! tx
                                 ["DELETE FROM suppliers WHERE id = ?" (uuid supplier-id)]
                                 {:return-keys true})))
    {:status 204}
    {:status 404 :body "Delete supplier failed without error."}))

;;; update supplier ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn patch-supplier
  [{{supplier-id :supplier-id} :route-params tx :tx data :body :as request}]
  (when (->> ["SELECT true AS exists FROM suppliers WHERE id = ?" (uuid supplier-id)]
             (jdbc-query tx)
             first :exists)
    (jdbc-update! tx :suppliers
                  (select-keys data fields)
                  ["id = ?" (uuid supplier-id)])
    {:status 204}))

;;; routes and paths ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn routes [request]
  (case (:request-method request)
    :get (get-supplier request)
    :patch (patch-supplier request)
    :delete (delete-supplier request)))

;#### debug ###################################################################

;(debug/wrap-with-log-debug #'data-url-img->buffered-image)
;(debug/wrap-with-log-debug #'buffered-image->data-url-img)
;(debug/wrap-with-log-debug #'resized-img)

;(debug/debug-ns *ns*)
