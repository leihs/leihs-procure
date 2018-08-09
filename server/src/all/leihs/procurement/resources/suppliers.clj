(ns leihs.procurement.resources.suppliers
  (:require [clj-logging-config.log4j :as logging-config]
            [clojure.java.jdbc :as jdbc]
            [clojure.string :as clj-str]
            [leihs.procurement.utils.sql :as sql]
            [logbug.debug :as debug]))

(def suppliers-base-query
  (-> (sql/select :suppliers.*)
      (sql/from :suppliers)))

(defn get-suppliers
  [context args _]
  (jdbc/query
    (-> context
        :request
        :tx)
    (let [terms (some-> args
                        :search_term
                        (clj-str/split #"\s+")
                        (->> (map #(str "%" % "%"))))
          offset (:offset args)
          limit (:limit args)]
      (sql/format
        (cond-> suppliers-base-query
          (not-empty terms)
            (sql/merge-where
              (into [:and]
                    (map (fn [term] ["~~*" (sql/call :unaccent :suppliers.name)
                                     (sql/call :unaccent term)])
                      terms)))
          offset (sql/offset offset)
          limit (sql/limit limit))))))

;#### debug ###################################################################
; (logging-config/set-logger! :level :debug)
; (logging-config/set-logger! :level :info)
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
; (debug/undebug-ns *ns*)
