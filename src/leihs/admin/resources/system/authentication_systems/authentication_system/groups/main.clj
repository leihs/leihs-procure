(ns leihs.admin.resources.system.authentication-systems.authentication-system.groups.main
  (:refer-clojure :exclude [str keyword])
  (:require
    [clojure.java.jdbc :as jdbc]
    [clojure.set :as set]
    [compojure.core :as cpj]
    [leihs.admin.common.membership.groups.main :refer [extend-with-membership]]
    [leihs.admin.paths :refer [path]]
    [leihs.admin.resources.groups.main :as groups]
    [leihs.admin.utils.jdbc :as utils.jdbc]
    [leihs.admin.utils.regex :as regex]
    [leihs.admin.utils.seq :as seq]
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.sql :as sql]
    [logbug.debug :as debug]))


(defn member-expr [authentication-system-id]
  [:exists
   (-> (sql/select true)
       (sql/from [:authentication_systems_groups :asgs])
       (sql/merge-where [:= :groups.id :asgs.group_id])
       (sql/merge-where [:= :asgs.authentication_system_id
                         authentication-system-id]))])

(defn groups-query
  [{{authentication-system-id :authentication-system-id} :route-params
    :as request}]
  (-> (groups/groups-query request)
      (extend-with-membership  (member-expr authentication-system-id) request)))

(defn groups-formated-query [request]
  (-> request groups-query sql/format))

(defn groups [{tx :tx :as request}]
  (let [query (groups-query request)
        offset (:offset query)]
    {:body
     {:groups (-> query sql/format
                  (->>
                    (jdbc/query tx)
                    (seq/with-index offset)
                    seq/with-page-index))}}))


;;; put-group ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn put-group [{tx :tx :as request
                  body :body
                  {authentication-system-id :authentication-system-id
                   group-id :group-id} :route-params}]
  (utils.jdbc/insert-or-update!
    tx :authentication_systems_groups
    ["authentication_system_id = ? AND group_id = ?" authentication-system-id group-id]
    {:authentication_system_id authentication-system-id :group_id group-id})
  {:status 204})


;;; remove-group ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn remove-group [{tx :tx :as request
                    {group-id :group-id
                     authentication-system-id :authentication-system-id} :route-params}]
  (if (= 1 (->> ["group_id = ? AND authentication_system_id = ?"
                 group-id authentication-system-id]
                (jdbc/delete! tx :authentication_systems_groups)
                first))
    {:status 204}
    (throw (ex-info "Remove authentication_systems_groups failed" {:status 409}))))


;;; routes ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def authentication-system-group-path
  (path :authentication-system-group
        {:authentication-system-id ":authentication-system-id"
         :group-id ":group-id"}))

(def authentication-system-groups-path
  (path :authentication-system-groups
        {:authentication-system-id ":authentication-system-id" }))

(def routes
  (-> (cpj/routes
        (cpj/PUT authentication-system-group-path [] #'put-group)
        (cpj/DELETE authentication-system-group-path [] #'remove-group)
        (cpj/GET authentication-system-groups-path [] #'groups))))


;#### debug ###################################################################


;(debug/debug-ns *ns*)
;(debug/debug-ns 'leihs.admin.resources.system.authentication-systems.authentication-system.groups.shared)
