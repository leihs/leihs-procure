(ns leihs.procurement.resources.suppliers
  (:require
   [clojure.string :as clj-str]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [next.jdbc :as jdbc]
   [taoensso.timbre :refer [debug error info spy warn]]))

(def suppliers-base-query
  (-> (sql/select :suppliers.*)
      (sql/from :suppliers)))

(defn get-suppliers
  [context args _]
  (jdbc/execute!
   (-> context
       :request
       :tx)
   (let [terms (some-> args
                       :search_term
                       (clj-str/split #"\s+")
                       (->> (map #(str "%" % "%"))))
         offset (:offset args)
         limit (:limit args)]
     (sql-format
      (cond-> suppliers-base-query
        (not-empty terms)
        (sql/where
         (into [:and]
               (map (fn [term] [:ilike (:unaccent :suppliers.name) (:unaccent term)])
                    terms)))
        offset (sql/offset offset)
        limit (sql/limit limit))))))
