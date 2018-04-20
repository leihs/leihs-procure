(ns leihs.procurement.resources.inspectors
  (:require [clj-logging-config.log4j :as logging-config]
            [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as logging]
            [leihs.procurement.resources.user :as user]
            [leihs.procurement.utils.ds :as ds]
            [leihs.procurement.utils.sql :as sql]
            [logbug.debug :as debug]))

(defn get-inspectors [context _ value]
  (jdbc/query (-> context :request :tx)
              (-> user/user-base-query
                  (sql/merge-where
                    [:in :users.id 
                     (-> (sql/select :pci.user_id)
                         (sql/from [:procurement_category_inspectors :pci])
                         (sql/merge-where [:= :pci.category_id "fc7bc38c-62f3-5576-802f-c874e06ee776"]))])
                  sql/format)))
              
;#### debug ###################################################################
; (logging-config/set-logger! :level :debug)
; (logging-config/set-logger! :level :info)
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
; (debug/undebug-ns *ns*)
