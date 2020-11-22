(ns leihs.admin.resources.users.user.groups
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.core.core :refer [keyword str presence]])
  (:require
    [leihs.core.sql :as sql]

    [leihs.admin.paths :refer [path]]
    [leihs.admin.resources.users.choose-core :as choose-core]

    [clojure.set :refer [rename-keys]]
    [clojure.java.jdbc :as jdbc]
    [compojure.core :as cpj]

    [clj-logging-config.log4j :as logging-config]
    [clojure.tools.logging :as logging]
    [logbug.debug :as debug]
    [logbug.catcher :as catcher]
    ))

(def users-count-subquery
  (-> (sql/select :%count.*)
      (sql/from :groups_users)
      (sql/where [:= :users.id :groups_users.user_id])
      (sql/where [:= :groups.id :groups_users.group_id])
      ))



(defn user-groups-query [user-id]
  (-> (sql/select :groups.name
                  [:groups.id :group_id]
                  :groups.org_id)
      (sql/from :users)
      (sql/merge-where [:= :users.id user-id])
      (sql/merge-join :groups_users [:= :users.id :groups_users.user_id])
      (sql/merge-join :groups [:= :groups_users.group_id :groups.id])
      (sql/merge-select [users-count-subquery :users_count])
      (sql/order-by :groups.name)
      sql/format
      ))

(defn groups [user-id tx]
  (->> user-id
       user-groups-query
       (jdbc/query tx)))

(defn user-groups
  [{tx :tx data :body {user-id :user-id} :route-params}]
  {:body
   {:user-groups
    (groups user-id tx)}})


;;; create user ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def routes
  (cpj/routes
    (cpj/GET (path :user-groups {:user-id ":user-id"}) [] #'user-groups)))


;#### debug ###################################################################
;(logging-config/set-logger! :level :debug)
;(debug/wrap-with-log-debug #'data-url-img->buffered-image)
;(debug/wrap-with-log-debug #'buffered-image->data-url-img)
;(debug/wrap-with-log-debug #'resized-img)
;(logging-config/set-logger! :level :info)
;(debug/debug-ns *ns*)
