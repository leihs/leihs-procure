(ns leihs.admin.resources.suppliers.supplier.main
  (:refer-clojure :exclude [str keyword])
  (:require
   [clojure.set :refer [rename-keys]]
   [compojure.core :as cpj]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.admin.paths :refer [path]]
   [leihs.core.auth.core :as auth]
   [leihs.core.core :refer [keyword str presence]]
   [leihs.core.uuid :refer [uuid]]
   [logbug.catcher :as catcher]
   [logbug.debug :as debug]
   [next.jdbc :as jdbc]
   [next.jdbc.sql :refer [query update! delete! insert!] :rename {query jdbc-query update! jdbc-update! delete! jdbc-delete! insert! jdbc-insert!}]
   [taoensso.timbre :refer [error warn info debug spy]]))

;;; data keys ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def fields #{:name :note})

;;; supplier ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn supplier-query [supplier-id]
  (-> (sql/select :id :name :note)
      (sql/from :suppliers)
      (sql/where [:= :id supplier-id])))

(defn supplier [tx-next supplier-id]
  (-> supplier-id
      uuid
      supplier-query
      sql-format
      (->> (jdbc-query tx-next))
      first))

(defn get-supplier
  [{tx-next :tx-next {supplier-id :supplier-id} :route-params}]
  {:body (supplier tx-next supplier-id)})

;;; delete group ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn delete-supplier
  [{tx-next :tx-next {supplier-id :supplier-id} :route-params}]
  (assert supplier-id)
  (if (= (uuid supplier-id)
         (:id (jdbc/execute-one! tx-next
                                 ["DELETE FROM suppliers WHERE id = ?" (uuid supplier-id)]
                                 {:return-keys true})))
    {:status 204}
    {:status 404 :body "Delete supplier failed without error."}))

;;; update supplier ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn patch-supplier
  [{{supplier-id :supplier-id} :route-params tx-next :tx-next data :body :as request}]
  (when (->> ["SELECT true AS exists FROM suppliers WHERE id = ?" (uuid supplier-id)]
             (jdbc-query tx-next)
             first :exists)
    (jdbc-update! tx-next :suppliers
                  (select-keys data fields)
                  ["id = ?" (uuid supplier-id)])
    {:status 204}))

;;; routes and paths ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def supplier-path (path :supplier {:supplier-id ":supplier-id"}))

(def routes
  (cpj/routes
   (cpj/GET supplier-path [] #'get-supplier)
   (cpj/PATCH supplier-path [] #'patch-supplier)
   (cpj/DELETE supplier-path [] #'delete-supplier)))

;#### debug ###################################################################

;(debug/wrap-with-log-debug #'data-url-img->buffered-image)
;(debug/wrap-with-log-debug #'buffered-image->data-url-img)
;(debug/wrap-with-log-debug #'resized-img)

;(debug/debug-ns *ns*)
