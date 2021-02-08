(ns leihs.admin.common.users-and-groups.core
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.core.core :refer [keyword str presence]])
  (:require
    [leihs.core.routing.back :as routing :refer []]
    [leihs.core.sql :as sql]

    [leihs.admin.paths :refer [path]]
    [leihs.admin.utils.seq :as seq]

    [clojure.java.jdbc :as jdbc]
    [clojure.set]
    [compojure.core :as cpj]

    [clj-logging-config.log4j :as logging-config]
    [clojure.tools.logging :as logging]
    [logbug.debug :as debug]
    ))


(defn org-id-filter [query request]
  (let [qp (presence (or (some-> request :query-params-raw :org_id)
                         (some-> request :query-params-raw :type)))]
    (case qp
      (nil "any") query
      ("true" "org") (sql/merge-where query [:<> nil :org_id])
      ("false" "manual") (sql/merge-where query [:= nil :org_id])
      (sql/merge-where query [:= :org_id (str qp)]))))

(defn organization-filter
  [query {{organization :organization} :query-params-raw :as request}]
  (case (presence organization)
    nil query
    "(none)" (sql/merge-where query [:= nil :organization])
    (sql/merge-where query [:= organization :organization])))

(defn protected-filter
  [query {{protected :protected} :query-params-raw :as request}]
  (case (presence protected)
    ("any" nil) query
    "none" (sql/merge-where
             query [:and
                    [:= false :system_admin_protected]
                    [:= false :admin_protected]])
    "admin" (sql/merge-where query [:= true :admin_protected])
    "system-admin" (sql/merge-where query [:= true :system_admin_protected])))


(defn assert-attributes-do-not-change!
  [data entity attributes]
  (when-let [attr (some
                    #(and (contains? data %)
                          (not= (get entity %) (get data %))
                          %)
                    attributes)]
    (throw (ex-info (str "Forbitten to change attribute " attr)
                    {:status 403 :attribure attr}))))

(defn assert-attributes-are-not-set!  [data attributes]
  (when-let [attr (some #(and (contains? data %) %) attributes)]
    (throw (ex-info (str "Forbitten to set the attribute " attr)
                    {:status 403 :attribure attr}))))

(defn assert-not-admin-proteced! [entity]
  (when (-> entity :admin_protected not false?)
    (throw
      (ex-info
        "Only admins may modify or delete admin-protected entities"
        {:status 403}))))

(defn assert-not-system-admin-proteced! [entity]
  (when (-> entity :system_admin_protected not false?)
    (throw
      (ex-info
        "Only system-admins may modify or delete system-admin-protected entities"
        {:status 403}))))


(defn protect-leihs-core! [entity]
  (when (= (:organization entity) "leihs-core")
    (throw (ex-info "The leihs-core organizations are protected/reserved!"
                    {:status 422}))))
