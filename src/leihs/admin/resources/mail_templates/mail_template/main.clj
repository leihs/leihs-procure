(ns leihs.admin.resources.mail-templates.mail-template.main
  (:require
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.core.uuid :refer [uuid]]
   [next.jdbc.sql :refer [query update!] :rename {query jdbc-query update! jdbc-update!}]))

;;; data keys ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def write-fields #{:body})

;;; mail-template ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn mail-template-query [mail-template-id]
  (-> (sql/select :*)
      (sql/from :mail_templates)
      (sql/where [:= :id mail-template-id])))

(defn mail-template [tx mail-template-id]
  (-> mail-template-id
      uuid
      mail-template-query
      sql-format
      (->> (jdbc-query tx))
      first))

(defn assert-global [template]
  (when-not (:is_template_template template)
    (throw (ex-info "This is not a global mail template (is_template_template)!"
                    {:status 403}))))

(defn get-mail-template
  [{tx :tx {mail-template-id :mail-template-id} :route-params}]
  (let [template (mail-template tx mail-template-id)]
    (assert-global template)
    {:body template}))

;;; update mail-template ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn patch-mail-template
  [{{mail-template-id :mail-template-id} :route-params tx :tx data :body :as request}]
  (let [template (mail-template tx mail-template-id)]
    (assert-global template))
  (when (->> ["SELECT true AS exists FROM mail_templates WHERE id = ?" (uuid mail-template-id)]
             (jdbc-query tx)
             first :exists)
    (jdbc-update! tx :mail_templates
                  (select-keys data write-fields)
                  ["id = ?" (uuid mail-template-id)])
    {:status 204}))

;;; routes and paths ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn routes [request]
  (case (:request-method request)
    :get (get-mail-template request)
    :patch (patch-mail-template request)))

;#### debug ###################################################################

;(debug/wrap-with-log-debug #'data-url-img->buffered-image)
;(debug/wrap-with-log-debug #'buffered-image->data-url-img)
;(debug/wrap-with-log-debug #'resized-img)

;(debug/debug-ns *ns*)
