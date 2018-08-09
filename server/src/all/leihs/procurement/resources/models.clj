(ns leihs.procurement.resources.models
  (:require [clj-logging-config.log4j :as logging-config]
            [clojure.java.jdbc :as jdbc]
            [clojure.string :as clj-str]
            [leihs.procurement.utils.sql :as sql]
            [logbug.debug :as debug]))

(def sql-name
  (sql/call :concat :product (sql/call :cast " " :varchar) :version))

(def models-base-query
  (-> (sql/select :* [sql-name :name])
      (sql/from :models)))

(defn get-models
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
        (cond-> models-base-query
          (not-empty terms)
            (sql/merge-where
              (into [:and]
                    (map (fn [term] ["~~*" (sql/call :unaccent sql-name)
                                     (sql/call :unaccent term)])
                      terms)))
          offset (sql/offset offset)
          limit (sql/limit limit))))))
