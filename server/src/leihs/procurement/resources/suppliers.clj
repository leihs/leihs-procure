(ns leihs.procurement.resources.suppliers
  (:require
    ;[clojure.java.jdbc :as jdbc]
    ;[leihs.procurement.utils.sql :as sql]

    [honey.sql :refer [format] :rename {format sql-format}]
    [leihs.core.db :as db]
    [next.jdbc :as jdbc]
    [honey.sql.helpers :as sql]

        [taoensso.timbre :refer [debug info warn error spy]]


    [clojure.string :as clj-str]
    [logbug.debug :as debug]))

(def suppliers-base-query
  (-> (sql/select :suppliers.*)
      (sql/from :suppliers)))

(defn get-suppliers
  [context args _]
  (jdbc/execute!
    (-> context
        :request
        :tx-next)
    (spy (let [terms (some-> args
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
                limit (sql/limit limit)))))))
