(ns leihs.admin.resources.mail-templates.main
  (:require
   [clojure.set]
   [compojure.core :as cpj]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.admin.paths :refer [path]]
   [leihs.admin.resources.mail-templates.shared :as shared]
   [leihs.admin.utils.seq :as seq]
   [leihs.core.core :refer [presence]]
   [logbug.debug :as debug]
   [next.jdbc.sql :as jdbc]
   [taoensso.timbre :refer [error warn info debug spy]]))

(def mail-templates-base-query
  (-> (sql/select :*)
      (sql/from :mail-templates)
      (sql/where [:= :is_template_template true])
      (sql/order-by :name :language_locale)))

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

(defn type-filter [query request]
  (if-let [mt-type (-> request :query-params-raw :type presence)]
    (-> query (sql/where [:= :mail-templates.type mt-type]))
    query))

(def searchable-expr
  [:concat :mail_templates.name " " :mail_templates.body])

(defn term-filter [query request]
  (if-let [term (-> request :query-params-raw :term presence)]
    (-> query (sql/where [:ilike searchable-expr (str "%" term "%")]))
    query))

(defn name-filter [query request]
  (if-let [mt-name (-> request :query-params-raw :name presence)]
    (-> query (sql/where [:= :mail-templates.name mt-name]))
    query))

(defn language-locale-filter [query request]
  (if-let [language-locale (-> request :query-params-raw :language_locale presence)]
    (-> query (sql/where [:= :mail-templates.language_locale language-locale]))
    query))

(defn mail-templates-query [request]
  (let [query-params (-> request :query-params
                         shared/normalized-query-parameters)]
    (-> mail-templates-base-query
        (set-per-page-and-offset query-params)
        (term-filter request)
        (name-filter request)
        (language-locale-filter request)
        (type-filter request))))

(defn mail-templates [{tx-next :tx-next :as request}]
  (let [query (mail-templates-query request)
        offset (:offset query)]
    {:body
     {:mail-templates (-> query
                          sql-format
                          (->> (jdbc/query tx-next)
                               (seq/with-index offset)
                               seq/with-page-index))}}))

;;; routes and paths ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def routes
  (cpj/routes
   (cpj/GET (path :mail-templates) [] #'mail-templates)))

;#### debug ###################################################################
;(debug/debug-ns *ns*)
;(debug/wrap-with-log-debug #'groups-formated-query)
