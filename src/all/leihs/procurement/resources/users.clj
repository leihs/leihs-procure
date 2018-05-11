(ns leihs.procurement.resources.users
  (:require [clj-logging-config.log4j :as logging-config]
            [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log]
            [leihs.procurement.utils.sql :as sql]
            [leihs.procurement.utils.ds :refer [get-ds]]
            [logbug.debug :as debug]))

(def users-base-query
  (-> (sql/select :users.*)
      (sql/from :users)))

(defn get-users
  [context args _]
  (jdbc/query
    (-> context
        :request
        :tx)
    (let [search-term (:search_term args)
          term-percent (and search-term (str "%" search-term "%"))
          offset (:offset args)
          limit (:limit args)]
      (sql/format (cond-> users-base-query
                    term-percent (sql/merge-where
                                   ["~~*"
                                    (->> (sql/call :concat
                                                   :users.firstname
                                                   (sql/call :cast " " :varchar)
                                                   :users.lastname)
                                         (sql/call :unaccent))
                                    (sql/call :unaccent term-percent)])
                    offset (sql/offset offset)
                    limit (sql/limit limit))))))

;#### debug ###################################################################
; (logging-config/set-logger! :level :debug)
; (logging-config/set-logger! :level :info)
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
; (debug/undebug-ns *ns*)
