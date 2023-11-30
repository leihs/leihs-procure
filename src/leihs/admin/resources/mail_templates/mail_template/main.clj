(ns leihs.admin.resources.mail-templates.mail-template.main
  (:refer-clojure :exclude [str keyword])
  (:require
   [clojure.set :refer [rename-keys]]
   [compojure.core :as cpj]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.admin.paths :refer [path]]
   [leihs.admin.resources.mail-templates.shared :as shared]
   [leihs.core.auth.core :as auth]
   [leihs.core.core :refer [keyword str presence]]
   [leihs.core.uuid :refer [uuid]]
   [logbug.catcher :as catcher]
   [logbug.debug :as debug]
   [next.jdbc :as jdbc]
   [next.jdbc.sql :refer [query update!] :rename {query jdbc-query update! jdbc-update!}]
   [taoensso.timbre :refer [error warn info debug spy]]))

;;; data keys ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def write-fields #{:body})

;;; mail-template ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn mail-template-query [mail-template-id]
  (-> (sql/select :*)
      (sql/from :mail_templates)
      (sql/where [:= :id mail-template-id])))

(defn mail-template [tx-next mail-template-id]
  (-> mail-template-id
      uuid
      mail-template-query
      sql-format
      (->> (jdbc-query tx-next))
      first))

(defn assert-global [template]
  (when-not (:is_template_template template)
    (throw (ex-info "This is not a global mail template (is_template_template)!"
                    {:status 403}))))

(defn get-mail-template
  [{tx-next :tx-next {mail-template-id :mail-template-id} :route-params}]
  (let [template (mail-template tx-next mail-template-id)]
    (assert-global template)
    {:body template}))

;;; update mail-template ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn patch-mail-template
  [{{mail-template-id :mail-template-id} :route-params tx-next :tx-next data :body :as request}]
  (let [template (mail-template tx-next mail-template-id)]
    (assert-global template))
  (when (->> ["SELECT true AS exists FROM mail_templates WHERE id = ?" (uuid mail-template-id)]
             (jdbc-query tx-next)
             first :exists)
    (jdbc-update! tx-next :mail_templates
                  (select-keys data write-fields)
                  ["id = ?" (uuid mail-template-id)])
    {:status 204}))

;;; routes and paths ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def mail-template-path
  (path :mail-template {:mail-template-id ":mail-template-id"}))

(def routes
  (cpj/routes
   (cpj/GET mail-template-path [] #'get-mail-template)
   (cpj/PATCH mail-template-path [] #'patch-mail-template)))

;#### debug ###################################################################

;(debug/wrap-with-log-debug #'data-url-img->buffered-image)
;(debug/wrap-with-log-debug #'buffered-image->data-url-img)
;(debug/wrap-with-log-debug #'resized-img)

;(debug/debug-ns *ns*)
