(ns leihs.procurement.permissions.user
  (:require [clj-logging-config.log4j :as logging-config]
            [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log]
            [leihs.procurement.utils.sql :as sql]
            [leihs.procurement.utils.ds :refer [get-ds]]
            [logbug.debug :as debug]))

(defn admin?
  [tx user]
  (:result
    (first (jdbc/query
             tx
             (-> (sql/select
                   [(sql/call :exists
                              (-> (sql/select true)
                                  (sql/from :procurement_admins)
                                  (sql/where [:= :procurement_admins.user_id
                                              (:id user)]))) :result])
                 sql/format)))))

(defn inspector?
  ([tx user] (inspector? tx user nil))
  ([tx user c-id]
   (:result
     (first
       (jdbc/query
         tx
         (-> (sql/select
               [(sql/call
                  :exists
                  (cond-> (-> (sql/select true)
                              (sql/from :procurement_category_inspectors)
                              (sql/merge-where
                                [:= :procurement_category_inspectors.user_id
                                 (:id user)]))
                    c-id (sql/merge-where
                           [:= :procurement_category_inspectors.category_id
                            c-id]))) :result])
             sql/format))))))

(defn viewer?
  [tx user]
  (:result
    (first (jdbc/query
             tx
             (-> (sql/select
                   [(sql/call
                      :exists
                      (-> (sql/select true)
                          (sql/from :procurement_category_viewers)
                          (sql/where [:= :procurement_category_viewers.user_id
                                      (:id user)]))) :result])
                 sql/format)))))

(defn requester?
  [tx user]
  (:result
    (first
      (jdbc/query
        tx
        (-> (sql/select
              [(sql/call :exists
                         (-> (sql/select true)
                             (sql/from :procurement_requesters_organizations)
                             (sql/where
                               [:= :procurement_requesters_organizations.user_id
                                (:id user)]))) :result])
            sql/format)))))

(defn requester-of?
  [tx user request]
  (= (str (:id user)) (str (:user_id request))))
