(ns leihs.admin.resources.system.authentication-systems.authentication-system.groups.main
  (:require
   [bidi.bidi :refer [match-route]]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.admin.common.membership.groups.main :refer [extend-with-membership]]
   [leihs.admin.paths :refer [paths]]
   [leihs.admin.resources.groups.main :as groups]
   [leihs.admin.utils.jdbc :as utils.jdbc]
   [leihs.admin.utils.seq :as seq]
   [next.jdbc :as jdbc]
   [next.jdbc.sql :refer [delete! query] :rename {query jdbc-query, delete! jdbc-delete!}]))

(defn member-expr [authentication-system-id]
  [:exists
   (-> (sql/select true)
       (sql/from [:authentication_systems_groups :asgs])
       (sql/where [:= :groups.id :asgs.group_id])
       (sql/where [:= :asgs.authentication_system_id
                   authentication-system-id]))])

(defn groups-query
  [{{authentication-system-id :authentication-system-id} :route-params
    :as request}]
  (-> (groups/groups-query request)
      (extend-with-membership  (member-expr authentication-system-id) request)))

(defn groups-formated-query [request]
  (-> request groups-query sql-format))

(defn groups [{tx :tx-next :as request}]
  (let [query (groups-query request)
        offset (:offset query)]
    {:body
     {:groups (-> query sql-format
                  (->>
                   (jdbc-query tx)
                   (seq/with-index offset)
                   seq/with-page-index))}}))

;;; put-group ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn put-group [{tx :tx-next :as request
                  body :body
                  {authentication-system-id :authentication-system-id
                   group-id :group-id} :route-params}]
  (utils.jdbc/insert-or-update!
   tx :authentication_systems_groups
   ["authentication_system_id = ? AND group_id = ?" authentication-system-id group-id]
   {:authentication_system_id authentication-system-id :group_id group-id})
  {:status 204})

;;; remove-group ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn remove-group [{tx :tx-next :as request
                     {group-id :group-id
                      authentication-system-id :authentication-system-id} :route-params}]
  (if (= 1 (->> ["group_id = ? AND authentication_system_id = ?"
                 group-id authentication-system-id]
                (jdbc-delete! tx :authentication_systems_groups)
                ::jdbc/update-count))
    {:status 204}
    (throw (ex-info "Remove authentication_systems_groups failed" {:status 409}))))

;;; routes ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn routes [request]
  (let [handler-key (->> request :uri (match-route paths) :handler)]
    (case [(:request-method request) handler-key]
      [:get :authentication-system-groups] (groups request)
      [:put :authentication-system-group] (put-group request)
      [:delete :authentication-system-group] (remove-group request))))

;#### debug ###################################################################

;(debug/debug-ns *ns*)
;(debug/debug-ns 'leihs.admin.resources.system.authentication-systems.authentication-system.groups.shared)
