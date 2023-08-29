(ns leihs.procurement.resources.template
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.data :refer [diff]]
            [clojure.test :as t]
            [leihs.core.core :refer [raise]]
            [leihs.procurement.utils.sql :as sql]))

(def templates-base-query
  (-> (sql/select :procurement_templates.*)
      (sql/from :procurement_templates)))

(defn get-template-by-id
  [tx id]
  (-> templates-base-query
      (sql/merge-where [:= :procurement_templates.id id])
      sql/format
      (->> (jdbc/query tx))
      first))

(defn insert-template!
  [tx tmpl]
  (jdbc/execute! tx
                 (-> (sql/insert-into :procurement_templates)
                     (sql/values [tmpl])
                     sql/format)))

(t/with-test
  (defn valid-update-maps-for-used-template? [tmpl-before tmpl-after]
    (let [[only-in-before only-in-after _] (diff tmpl-before tmpl-after)]
      (or (and (empty? only-in-before) (empty? only-in-after))
          (and (= (keys only-in-before) [:is_archived])
               (= (keys only-in-after) [:is_archived])))))
  (t/is (= (valid-update-maps-for-used-template? {:is_archived false}
                                                 {:is_archived true})
           true)
        "True if only is_archived has been provided and has changed too.")
  (t/is (= (valid-update-maps-for-used-template? {:is_archived true, :article_name "foo"}
                                                 {:is_archived false, :article_name "foo"})
           true)
        "True if among others only is_archived attribute has changed.")
  (t/is (= (valid-update-maps-for-used-template? {:is_archived true, :article_name "foo"}
                                                 {:is_archived false, :article_name "bar"})
           false)
        "False if also some other attribute has changed.")
  (t/is (= (valid-update-maps-for-used-template? {:is_archived true, :article_name "foo"}
                                                 {:is_archived true, :article_name "foo"})
           true)
        "True if nothing has changed."))

; (test #'valid-update-maps-for-used-template?)

(defn validate-update-attributes! [tx tmpl-after]
  (let [tmpl-before (get-template-by-id tx (:id tmpl-after))
        req-exist? (-> (sql/select (sql/call :count :*))
                       (sql/from :procurement_requests)
                       (sql/where [:= :template_id (:id tmpl-before)])
                       (->> (jdbc/query tx))
                       first :count (> 0))]
    (when (and req-exist?
               (not (valid-update-maps-for-used-template? tmpl-before tmpl-after)))
      (raise "A used template can only be archived or unarchived."))))

(defn update-template!
  [tx tmpl]
  (validate-update-attributes! tx tmpl)
  (jdbc/execute! tx
                 (-> (sql/update :procurement_templates)
                     (sql/sset tmpl)
                     (sql/where [:= :procurement_templates.id (:id tmpl)])
                     sql/format)))

(defn delete-template!
  [tx id]
  (jdbc/execute! tx
                 (-> (sql/delete-from :procurement_templates)
                     (sql/where [:= :procurement_templates.id id])
                     sql/format)))

(defn get-template
  ([context _ value]
   (get-template-by-id (-> context
                           :request
                           :tx)
                       (or (:value value) ; for RequestFieldTemplate
                           (:template_id value))))
  ([tx tmpl]
   (let [where-clause (sql/map->where-clause :procurement_templates tmpl)]
     (-> templates-base-query
         (sql/merge-where where-clause)
         sql/format
         (->> (jdbc/query tx))
         first))))

(defn can-delete?
  [context _ value]
  (-> (sql/call
        :not
        (sql/call :exists
                  (-> (sql/select true)
                      (sql/from [:procurement_requests :pr])
                      (sql/merge-where [:= :pr.template_id (:id value)]))))
      (vector :result)
      sql/select
      sql/format
      (->> (jdbc/query (-> context
                           :request
                           :tx)))
      first
      :result))

(def can-update? can-delete?)
