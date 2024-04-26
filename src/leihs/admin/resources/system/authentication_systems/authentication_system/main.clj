(ns leihs.admin.resources.system.authentication-systems.authentication-system.main
  (:require
   [bidi.bidi :refer [match-route]]
   [clojure.core.match :refer [match]]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.admin.paths :refer [paths]]
   [next.jdbc :as jdbc]
   [next.jdbc.sql :refer [delete! insert! query update!] :rename {query jdbc-query,
                                                                  insert! jdbc-insert!,
                                                                  update! jdbc-update!,
                                                                  delete! jdbc-delete!}]
   [taoensso.timbre :refer [debug spy]]))

;;; data keys ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def authentication-system-selects
  [:authentication-systems.*
   [(-> (sql/select :%count.*)
        (sql/from :authentication_systems_users)
        (sql/where [:= :authentication-systems_users.authentication-system_id :authentication-systems.id]))
    :users_count]
   [(-> (sql/select :%count.*)
        (sql/from :authentication_systems_groups)
        (sql/where [:= :authentication-systems_groups.authentication-system_id :authentication-systems.id]))
    :groups_count]])

;;; authentication-system ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn authentication-system-query [authentication-system-id]
  (-> (apply sql/select authentication-system-selects)
      (sql/from :authentication-systems)
      (sql/where [:= :id authentication-system-id])
      sql-format))

(defn authentication-system
  [{tx :tx-next {authentication-system-id :authentication-system-id} :route-params}]
  {:body
   (first (jdbc-query tx (authentication-system-query authentication-system-id)))})

;;; delete authentication-system ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn delete-authentication-system
  [{tx :tx-next {authentication-system-id :authentication-system-id} :route-params}]
  (assert authentication-system-id)
  (if (= 1 (::jdbc/update-count
            (jdbc-delete! tx :authentication_systems ["id = ?" authentication-system-id])))
    {:status 204}
    {:status 404 :body "Delete authentication-system failed without error."}))

;;; update authentication-system ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn patch-authentication-system
  ([{tx :tx-next data :body {authentication-system-id :authentication-system-id} :route-params}]
   (patch-authentication-system authentication-system-id data tx))
  ([authentication-system-id data tx]
   (when (->> ["SELECT true AS exists FROM authentication_systems WHERE id = ?" authentication-system-id]
              (jdbc-query tx)
              first :exists)
     (jdbc-update! tx :authentication_systems
                   (dissoc data :id :groups_count :updated_at :created_at)
                   ["id = ?" authentication-system-id])
     {:status 204})))

;;; create authentication-system ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn create-authentication-system
  ([{tx :tx-next data :body}]
   (create-authentication-system data tx))
  ([data tx]
   (if-let [authentication-system (jdbc-insert! tx :authentication_systems data)]
     {:status 201, :body authentication-system}
     {:status 422
      :body "No authentication-system has been created."})))

;;; routes and paths ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn routes [request]
  (let [handler-key (->> request :uri (match-route paths) :handler)]
    (match [(:request-method request) handler-key]
      [:get :authentication-system] (authentication-system request)
      [:patch :authentication-system] (patch-authentication-system request)
      [:delete :authentication-system] (delete-authentication-system request)
      [:post :authentication-systems] (create-authentication-system request))))

;#### debug ###################################################################

;(debug/wrap-with-log-debug #'data-url-img->buffered-image)
;(debug/wrap-with-log-debug #'buffered-image->data-url-img)
;(debug/wrap-with-log-debug #'resized-img)

;(debug/debug-ns *ns*)
