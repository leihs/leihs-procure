(ns leihs.procurement.resources.models
  (:require
    [clojure.string :as clj-str]
    [honey.sql :refer [format] :rename {format sql-format}]
    [honey.sql.helpers :as sql]
    [next.jdbc :as jdbc]))

(def sql-name
  (concat :product (:cast " " :varchar) :version))

(def models-base-query
  (-> (sql/select :* [sql-name :name])
      (sql/from :models)))

(defn get-models
  [context args _]
  (jdbc/execute!
    (-> context
        :request
        :tx-next)
    (let [terms (some-> args
                        :search_term
                        (clj-str/split #"\s+")
                        (->> (map #(str "%" % "%"))))
          offset (:offset args)
          limit (:limit args)]
      (sql-format
        (cond-> models-base-query
          (not-empty terms)
            (sql/where
              (into [:and]
                    (map (fn [term] [:ilike (:unaccent sql-name) (:unaccent term)])
                      terms)))
          offset (sql/offset offset)
          limit (sql/limit limit))))))
