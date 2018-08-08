(ns leihs.procurement.resources.users
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.string :as clj-str]
            [leihs.procurement.utils.sql :as sql]))

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
          term-parts (and search-term
                          (map (fn [part] (str "%" part "%"))
                            (clj-str/split search-term #"\s+")))
          exclude-ids (:exclude_ids args)
          offset (:offset args)
          limit (:limit args)]
      (sql/format
        (cond-> users-base-query
          term-parts (sql/merge-where
                       (into [:and]
                             (map (fn [term-percent]
                                    ["~~*"
                                     (->>
                                       (sql/call :concat
                                                 :users.firstname
                                                 (sql/call :cast " " :varchar)
                                                 :users.lastname)
                                       (sql/call :unaccent))
                                     (sql/call :unaccent term-percent)])
                               term-parts)))
          exclude-ids (sql/merge-where [:not-in :users.id exclude-ids])
          offset (sql/offset offset)
          limit (sql/limit limit))))))

;#### debug ###################################################################
; (logging-config/set-logger! :level :debug)
; (logging-config/set-logger! :level :info)
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
; (debug/undebug-ns *ns*)
