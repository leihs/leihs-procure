(ns leihs.admin.resources.buildings.building.main
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.core.core :refer [keyword str presence]])
  (:require
    [clojure.set :refer [rename-keys]]
    [compojure.core :as cpj]
    [honey.sql :refer [format] :rename {format sql-format}]
    [honey.sql.helpers :as sql]
    [leihs.admin.paths :refer [path]]
    [leihs.admin.resources.buildings.shared :as shared]
    [leihs.core.auth.core :as auth]
    [leihs.core.uuid :refer [uuid]]
    [logbug.catcher :as catcher]
    [logbug.debug :as debug]
    [next.jdbc :as jdbc]
    [next.jdbc.sql :refer [query update! delete! insert!] :rename {query jdbc-query update! jdbc-update! delete! jdbc-delete! insert! jdbc-insert!}]
    [taoensso.timbre :refer [error warn info debug spy]]
    ))

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

(def building-path (path :building {:building-id ":building-id"}))

(def routes
  (cpj/routes
    (cpj/GET building-path [] #'get-building)
    (cpj/PATCH building-path [] #'patch-building)
    (cpj/DELETE building-path [] #'delete-building)))

;#### debug ###################################################################

;(debug/wrap-with-log-debug #'data-url-img->buffered-image)
;(debug/wrap-with-log-debug #'buffered-image->data-url-img)
;(debug/wrap-with-log-debug #'resized-img)

;(debug/debug-ns *ns*)
