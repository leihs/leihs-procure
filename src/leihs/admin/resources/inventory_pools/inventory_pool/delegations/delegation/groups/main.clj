(ns leihs.admin.resources.inventory-pools.inventory-pool.delegations.delegation.groups.main
  (:refer-clojure :exclude [str keyword])
  (:require
   [bidi.bidi :refer [match-route]]
   [clojure.core.match :refer [match]]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.admin.common.membership.groups.main :refer [extend-with-membership]]
   [leihs.admin.paths :refer [paths]]
   [leihs.admin.resources.groups.main :as groups]
   [leihs.admin.utils.jdbc :as utils.jdbc]
   [leihs.admin.utils.seq :as seq]
   [next.jdbc :as jdbc]
   [next.jdbc.sql :refer [delete! query] :rename {query jdbc-query, delete! jdbc-delete!}]))

(defn member-expr [delegation-id]
  [:exists
   (-> (sql/select true)
       (sql/from [:delegations_groups :dgs])
       (sql/where [:= :groups.id :dgs.group_id])
       (sql/where [:= :dgs.delegation_id delegation-id]))])

(defn groups-query
  [{{delegation-id :delegation-id} :route-params :as request}]
  (-> (groups/groups-query request)
      (extend-with-membership  (member-expr delegation-id) request)))

(defn groups [{tx :tx-next :as request}]
  (let [query (groups-query request)
        offset (:offset query)]
    {:body
     {:groups (-> query sql-format
                  (->>
                   (jdbc-query tx)
                   (seq/with-index offset)
                   seq/with-page-index))}}))

;;; add ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn add-group
  [{{delegation-id :delegation-id
     group-id :group-id} :route-params
    tx :tx-next :as request}]
  (utils.jdbc/insert-or-update!
   tx :delegations_groups
   ["delegation_id = ? AND group_id = ?  " delegation-id group-id]
   {:delegation_id delegation-id :group_id group-id})
  {:status 204})

;;; remove ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn remove-group
  [{{delegation-id :delegation-id
     group-id :group-id} :route-params
    tx :tx-next :as request}]
  (if (= 1 (::jdbc/update-count
            (jdbc-delete! tx :delegations_groups
                          ["delegation_id= ? AND group_id = ?
                           " delegation-id group-id])))
    {:status 204}
    {:status 404 :body "Remove group failed without error."}))

;;; routes ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn routes [request]
  (let [handler-key (->> request :uri (match-route paths) :handler)]
    (match [(:request-method request) handler-key]
      [:get :inventory-pool-delegation-groups] (groups request)
      [:delete :inventory-pool-delegation-group] (remove-group request)
      [:put :inventory-pool-delegation-group] (add-group request))))

;#### debug ###################################################################

;(debug/wrap-with-log-debug #'groups-formated-query)
;(debug/debug-ns *ns*)
