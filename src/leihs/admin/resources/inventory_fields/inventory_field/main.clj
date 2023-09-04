(ns leihs.admin.resources.inventory-fields.inventory-field.main
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.core.core :refer [dissoc-in raise keyword str presence deep-merge]])
  (:require
    [clojure.set :refer [rename-keys intersection]]
    [clojure.spec.alpha :as spec]
    [compojure.core :as cpj]
    [honey.sql :refer [format] :rename {format sql-format}]
    [honey.sql.helpers :as sql]
    [leihs.admin.paths :refer [path]]
    [leihs.admin.resources.inventory-fields.inventory-field.specs :as field-specs]
    [leihs.core.auth.core :as auth]
    [leihs.core.db :as db]
    [leihs.core.uuid :refer [uuid]]
    [logbug.catcher :as catcher]
    [logbug.debug :as debug]
    [next.jdbc :as jdbc]
    [next.jdbc.sql :refer [query update! delete! insert!] :rename {query jdbc-query update! jdbc-update! delete! jdbc-delete! insert! jdbc-insert!}]
    [taoensso.timbre :refer [error warn info debug spy]]
    ))

;;; inventory-field ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn inventory-field-query [inventory-field-id]
  (-> (sql/select :id :active :position :data :dynamic)
      (sql/from :fields)
      (sql/where [:= :id inventory-field-id])))

(defn inventory-field [tx-next inventory-field-id]
  (-> inventory-field-id
      inventory-field-query
      sql-format
      (->> (jdbc-query tx-next))
      first))

(comment (inventory-field (db/get-ds-next) "attachments"))

(defn usage [tx-next inventory-field]
  (let [property (-> inventory-field :data :attribute second)]
    (-> (sql/select :%count.*)
        (sql/from :items)
        (sql/where [:raw (format "items.properties::json->>'%s' IS NOT NULL" property)])
        sql-format
        (->> (jdbc-query tx-next))
        first
        :count)))

(defn get-inventory-field
  [{tx-next :tx-next {inventory-field-id :inventory-field-id} :route-params}]
  (let [inventory-field (inventory-field tx-next inventory-field-id)]
    {:body {:inventory-field-data inventory-field
            :inventory-field-usage (when (:dynamic inventory-field)
                                     (usage tx-next inventory-field))}}))

;;; delete inventory-field ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn delete-inventory-field
  [{tx-next :tx-next {inventory-field-id :inventory-field-id} :route-params}]
  (when-let [field (inventory-field tx-next (assert inventory-field-id))]
    (when (or (-> field :dynamic not)
              (-> field :data :required)
              (->> field (usage tx-next) (> 0)))
      (raise "This inventory-field cannot be deleted.")))
  (if (= inventory-field-id
         (:id (jdbc/execute-one! tx-next
                                 ["DELETE FROM fields WHERE id = ?" inventory-field-id]
                                 {:return-keys true})))
    {:status 204}
    {:status 404 :body "Delete inventory-field failed without error."}))

;;; update inventory-field ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn validated-merge [tx-next old-f new-f]
  (let [dynamic? (:dynamic old-f)
        required? (-> old-f :data :required boolean)
        field-usage (usage tx-next old-f)
        spec (case [dynamic? required?]
               [true true] ::field-specs/dynamic-required-field
               [true false] ::field-specs/dynamic-not-required-field
               [false true] ::field-specs/core-required-field
               [false false] ::field-specs/core-not-required-field)]

    (spec/assert spec new-f)

    (when (and (#{::field-specs/dynamic-required-field
                  ::field-specs/dynamic-not-required-field}
                 spec)
               (> field-usage 0))

      (when (not= (-> old-f :data :type) (-> new-f :data :type))
        (raise "This inventory-field has already been used so the type can't be changed."))

      (when-not (intersection (-> old-f :data :values set) (-> new-f :data :values set))
        (raise "This inventory-field has already been used so the existing values must remain.")))

    (let [new-f* (deep-merge old-f new-f)]
      (cond-> new-f* (-> new-f* :data :target_type (= "any"))
        (dissoc-in [:data :target_type])))))

(defn patch-inventory-field
  [{{inventory-field-id :inventory-field-id} :route-params
    tx-next :tx-next body-data :body :as request}]
  (when-let [field (inventory-field tx-next inventory-field-id)]
    (jdbc-update! tx-next :fields
                  (validated-merge tx-next field body-data)
                  ["id = ?" inventory-field-id])
    {:status 204}))

;;; routes and paths ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def inventory-field-path
  (path :inventory-field {:inventory-field-id ":inventory-field-id"}))

(def routes
  (cpj/routes
    (cpj/GET inventory-field-path [] #'get-inventory-field)
    (cpj/PATCH inventory-field-path [] #'patch-inventory-field)
    (cpj/DELETE inventory-field-path [] #'delete-inventory-field)))

;#### debug ###################################################################

;(debug/wrap-with-log-debug #'data-url-img->buffered-image)
;(debug/wrap-with-log-debug #'buffered-image->data-url-img)
;(debug/wrap-with-log-debug #'resized-img)

;(debug/debug-ns *ns*)
