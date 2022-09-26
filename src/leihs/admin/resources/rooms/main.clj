(ns leihs.admin.resources.rooms.main
  (:refer-clojure :exclude [str keyword])
  (:require
    [next.jdbc.sql :as jdbc]
    [clojure.set]
    [compojure.core :as cpj]
    [leihs.admin.paths :refer [path]]
    [leihs.admin.resources.rooms.shared :as shared]
    [leihs.admin.resources.rooms.room.main :as room]
    [leihs.admin.utils.seq :as seq]
    [leihs.core.uuid :refer [uuid]]
    [leihs.core.core :refer [keyword str presence]]
    [honey.sql :refer [format] :rename {format sql-format}]
    [honey.sql.helpers :as sql]
    [logbug.debug :as debug]
    [taoensso.timbre :refer [error warn info debug spy]]
    ))

(def count-items-select-query
  (-> (sql/select :%count.*)
      (sql/from :items)
      (sql/join [:rooms :rooms_2] [:= :rooms_2.id :items.room_id])
      (sql/where := :items.room_id :rooms.id)))

(def select-fields
  (conj (map #(->> % name (str "rooms.") keyword)
             shared/default-fields)
        [count-items-select-query :items_count]))

(def rooms-base-query
  (-> (apply sql/select select-fields)
      (sql/from :rooms)
      (sql/join :buildings [:= :rooms.building_id :buildings.id])
      (sql/order-by :rooms.name)))

(defn set-per-page-and-offset
  ([query {per-page :per-page page :page}]
   (when (or (-> per-page presence not)
             (-> per-page integer? not)
             (> per-page 1000)
             (< per-page 1))
     (throw (ex-info "The query parameter per-page must be present and set to an integer between 1 and 1000."
                     {:status 422})))
   (when (or (-> page presence not)
             (-> page integer? not)
             (< page 0))
     (throw (ex-info "The query parameter page must be present and set to a positive integer."
                     {:status 422})))
   (set-per-page-and-offset query per-page page))
  ([query per-page page]
   (-> query
       (sql/limit per-page)
       (sql/offset (* per-page (- page 1))))))

(def searchable-expr [:concat :rooms.name " " :rooms.description])

(defn term-filter [query request]
  (if-let [term (-> request :query-params-raw :term presence)]
    (-> query
        (sql/where [:ilike searchable-expr (str "%" term "%")]))
    query))

(defn building-filter [query request]
  (if-let [building-id (-> request :query-params-raw :building_id presence)]
    (-> query
        (sql/where [:= :buildings.id (uuid building-id)]))
    query))

(defn general-filter [query request]
  (if-let [general (-> request :query-params-raw :general presence)]
    (case general
      "yes" (sql/where query [:= :rooms.general true])
      "no" (sql/where query [:= :rooms.general false])
      :else query)
    query))

(defn rooms-query [request]
  (let [query-params  (-> request :query-params
                          shared/normalized-query-parameters)]
    (-> rooms-base-query
        (set-per-page-and-offset query-params)
        (term-filter request)
        (building-filter request)
        (general-filter request))))

(defn rooms [{tx-next :tx-next :as request}]
  (let [query (rooms-query request)
        offset (:offset query)]
    {:body
     {:rooms (-> query
                 sql-format
                 (->> (jdbc/query tx-next)
                      (seq/with-index offset)
                      seq/with-page-index))}}))

;;; create room ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn create-room [{tx-next :tx-next data :body :as request}]
  (if-let [room (jdbc/insert! tx-next
                              :rooms
                              (-> data
                                  (select-keys shared/default-fields)
                                  (update :building_id uuid)))]
    {:body room}
    {:status 422
     :body "No room has been created."}))

;;; routes and paths ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def rooms-path (path :rooms))

(def routes
  (cpj/routes
    (cpj/GET rooms-path [] #'rooms)
    (cpj/POST rooms-path [] #'create-room)))

;#### debug ###################################################################
;(debug/debug-ns *ns*)
;(debug/wrap-with-log-debug #'groups-formated-query)
