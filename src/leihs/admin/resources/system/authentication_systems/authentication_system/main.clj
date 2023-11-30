(ns leihs.admin.resources.system.authentication-systems.authentication-system.main
  (:refer-clojure :exclude [str keyword])
  (:require
   [clojure.java.jdbc :as jdbc]
   [clojure.set :refer [rename-keys]]
   [compojure.core :as cpj]
   [leihs.admin.paths :refer [path]]
   [leihs.core.core :refer [keyword str presence]]
   [leihs.core.sql :as sql]
   [logbug.catcher :as catcher]
   [logbug.debug :as debug])
  (:import
   [java.awt.image BufferedImage]
   [java.io ByteArrayInputStream ByteArrayOutputStream]
   [java.util Base64]
   [javax.imageio ImageIO]))

;;; data keys ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def authentication-system-selects
  [:authentication-systems.*
   [(-> (sql/select :%count.*)
        (sql/from :authentication_systems_users)
        (sql/merge-where [:= :authentication-systems_users.authentication-system_id :authentication-systems.id]))
    :users_count]
   [(-> (sql/select :%count.*)
        (sql/from :authentication_systems_groups)
        (sql/merge-where [:= :authentication-systems_groups.authentication-system_id :authentication-systems.id]))
    :groups_count]])

;;; authentication-system ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn authentication-system-query [authentication-system-id]
  (-> (apply sql/select authentication-system-selects)
      (sql/from :authentication-systems)
      (sql/merge-where [:= :id authentication-system-id])
      sql/format))

(defn authentication-system [{tx :tx {authentication-system-id :authentication-system-id} :route-params}]
  {:body
   (first (jdbc/query tx (authentication-system-query authentication-system-id)))})

;;; delete authentication-system ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn delete-authentication-system [{tx :tx {authentication-system-id :authentication-system-id} :route-params}]
  (assert authentication-system-id)
  (if (= [1] (jdbc/delete! tx :authentication_systems ["id = ?" authentication-system-id]))
    {:status 204}
    {:status 404 :body "Delete authentication-system failed without error."}))

;;; update authentication-system ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn patch-authentication-system
  ([{tx :tx data :body {authentication-system-id :authentication-system-id} :route-params}]
   (patch-authentication-system authentication-system-id data tx))
  ([authentication-system-id data tx]
   (when (->> ["SELECT true AS exists FROM authentication_systems WHERE id = ?" authentication-system-id]
              (jdbc/query tx)
              first :exists)
     (jdbc/update! tx :authentication_systems
                   (dissoc data :id :groups_count) ["id = ?" authentication-system-id])
     {:status 204})))

;;; create authentication-system ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn create-authentication-system
  ([{tx :tx data :body}]
   (create-authentication-system data tx))
  ([data tx]
   (if-let [authentication-system (first (jdbc/insert! tx :authentication_systems data))]
     {:body authentication-system}
     {:status 422
      :body "No authentication-system has been created."})))

;;; routes and paths ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def authentication-system-path (path :authentication-system {:authentication-system-id ":authentication-system-id"}))

(def authentication-system-transfer-path
  (path :authentication-system-transfer-data {:authentication-system-id ":authentication-system-id"
                                              :target-authentication-system-id ":target-authentication-system-id"}))

(def routes
  (->
   (cpj/routes
    (cpj/GET authentication-system-path [] #'authentication-system)
    (cpj/PATCH authentication-system-path [] #'patch-authentication-system)
    (cpj/DELETE authentication-system-path [] #'delete-authentication-system)
    (cpj/POST (path :authentication-systems) [] #'create-authentication-system))))

;#### debug ###################################################################

;(debug/wrap-with-log-debug #'data-url-img->buffered-image)
;(debug/wrap-with-log-debug #'buffered-image->data-url-img)
;(debug/wrap-with-log-debug #'resized-img)

;(debug/debug-ns *ns*)
