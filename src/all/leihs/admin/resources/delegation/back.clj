(ns leihs.admin.resources.delegation.back
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.admin.utils.core :refer [keyword str presence]])
  (:require
    [leihs.admin.paths :refer [path]]
    [leihs.admin.utils.sql :as sql]
    [leihs.admin.resources.users.back :as users]

    [clojure.set :refer [rename-keys]]
    [clojure.java.jdbc :as jdbc]
    [compojure.core :as cpj]

    [clj-logging-config.log4j :as logging-config]
    [clojure.tools.logging :as logging]
    [logbug.debug :as debug]
    [logbug.catcher :as catcher]
    )
  (:import
    [java.awt.image BufferedImage]
    [java.io ByteArrayInputStream ByteArrayOutputStream]
    [java.util Base64]
    [javax.imageio ImageIO]
    ))

;;; data keys ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def delegation-selects
  [:created_at
   [:delegator_user_id :responsible_user_id]
   [:firstname :name]
   :id
   :updated_at
   [(-> (sql/select :%count.*)
        (sql/from :contracts)
        (sql/merge-where [:= :contracts.user_id :users.id]))
    :contracts_count]])

(def delegation-write-keys
  [:name
   :responsible_user_id])

(def delegation-write-keymap
  {:name :firstname
   :responsible_user_id :delegator_user_id})


;;; delegation ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn delegation-query [delegation-id]
  (-> (apply sql/select delegation-selects)
      (sql/from :users)
      (sql/merge-where [:= :id delegation-id])
      (sql/merge-where [:<> :delegator_user_id nil])
      sql/format))

(defn delegation [{tx :tx {delegation-id :delegation-id} :route-params}]
  {:body
   (first (jdbc/query tx (delegation-query delegation-id)))})

;;; delete delegation ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn delete-delegation [{tx :tx {delegation-id :delegation-id} :route-params}]
  (if (= [1] (jdbc/delete! tx :users ["id = ?" delegation-id]))
    {:status 204}
    {:status 404 :body "Delete delegation failed without error."}))


;;; update delegation ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn prepare-write-data [data tx]
  (catcher/with-logging
    {}
    (-> data
        (select-keys delegation-write-keys)
        (rename-keys delegation-write-keymap))))

(defn patch-delegation
  ([{tx :tx data :body {delegation-id :delegation-id} :route-params}]
   (patch-delegation delegation-id (prepare-write-data data tx) tx))
  ([delegation-id data tx]
   (when (->> ["SELECT true AS exists FROM users WHERE id = ? AND delegator_user_id IS NOT NULL" delegation-id]
              (jdbc/query tx)
              first :exists)
     (jdbc/update! tx :users data ["id = ?" delegation-id])
     {:status 204})))


;;; create delegation ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn create-delegation
  ([{tx :tx data :body}]
   (create-delegation (prepare-write-data data tx) tx))
  ([data tx]
   (let [delegation (or (first (jdbc/insert! tx :users data))
                        (throw (ex-info "Delegation has not been created" {:status 422
                                                                           :data data})))]
     (or (first (jdbc/insert! tx :delegations_users {:delegation_id (:id delegation)
                                                     :user_id (:delegator_user_id data)}))
         (throw (ex-info "Responsible user was not added" {:status 422
                                                           :data data})))
     {:body delegation})))

;;; routes and paths ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def delegation-path (path :delegation {:delegation-id ":delegation-id"}))

(def routes
  (cpj/routes
    (cpj/GET (path :delegation-add-choose-responsible-user
                   {:delegation-id ":delegation-id"})
             [] #'users/users)
    (cpj/GET (path :delegation-edit-choose-responsible-user
                   {:delegation-id ":delegation-id"})
             [] #'users/users)
    (cpj/GET delegation-path [] #'delegation)
    (cpj/PATCH delegation-path [] #'patch-delegation)
    (cpj/DELETE delegation-path [] #'delete-delegation)
    (cpj/POST (path :delegations) [] #'create-delegation)))


;#### debug ###################################################################
;(logging-config/set-logger! :level :debug)
;(debug/wrap-with-log-debug #'data-url-img->buffered-image)
;(debug/wrap-with-log-debug #'buffered-image->data-url-img)
;(debug/wrap-with-log-debug #'resized-img)
;(logging-config/set-logger! :level :info)
;(debug/debug-ns 'cider-ci.utils.shutdown)
;(debug/debug-ns *ns*)
