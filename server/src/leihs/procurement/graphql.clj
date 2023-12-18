(ns leihs.procurement.graphql
  (:require
    [clojure.edn :as edn]

    (java.sql.Date)
    ;(clojure.java [io :as io] [jdbc :as jdbco])
    (clojure.java [io :as io])



    ;; all needed imports
    [honey.sql :refer [format] :rename {format sql-format}]
    [leihs.core.db :as db]
    [next.jdbc :as jdbc]
    [honey.sql.helpers :as sql]

    [clojure.data.json :as json]

    [com.walmartlabs.lacinia :as lacinia]
    (com.walmartlabs.lacinia [parser :as graphql-parser]
                             [schema :as graphql-schema] [util :as graphql-util])
    [leihs.core.graphql :as core-graphql]
    [leihs.procurement.graphql.helpers :as helpers]
    [leihs.procurement.graphql.resolver :as resolver]
    [leihs.procurement.utils.ring-exception :refer [get-cause]]
    [taoensso.timbre :refer [debug error info spy warn]]
    ))

(def CUSTOM_SCALARS
  {:ID {:parse identity :serialize str}
   :Int {:parse (fn [v]
                  (if (number? v) v (Integer/parseInt v)))
         :serialize identity}})

(defn load-schema! []
  (or (some-> (io/resource "schema.edn")
              slurp edn/read-string
              (graphql-util/attach-resolvers resolver/resolvers)
              (assoc-in [:scalars] CUSTOM_SCALARS)
              graphql-schema/compile)
      (throw (ex-info "Failed to load schema" {}))))

(defn exec-query
  [query-string request]
  (debug "graphql query" query-string
         "with variables" (-> request
                              :body
                              :variables))
  (lacinia/execute (core-graphql/schema)
                   query-string
                   (-> request
                       :body
                       :variables)
                   {:request request}))

(defn pure-handler
  [{{query :query} :body, :as request}]
  (let [result (exec-query query request)
        resp {:body result}]
    (if (:errors result)
      (do (debug result) (assoc resp :graphql-error true))
      resp)))

(defn parse-query-with-exception-handling
  [schema query]
  (try (graphql-parser/parse-query schema query)
       (catch Throwable e*
         (let [e (get-cause e*)
               m (.getMessage e*)
               n (-> e*
                     .getClass
                     .getSimpleName)]
           (warn (or m n))
           (debug e)
           (helpers/error-as-graphql-object "API_ERROR" m)))))

(defn handler
  [{{query :query} :body, :as request}]
  (let [mutation? (->> query
                       (parse-query-with-exception-handling (core-graphql/schema))
                       graphql-parser/operations
                       :type
                       (= :mutation))]
    (if mutation?
      ;(jdbc/with-transaction
      (jdbc/with-transaction+options

        ;[tx-next (get-ds-next)]
                                          ;[
                                          ;p (println ">o> abc" (spy request))
                                          ;tx (:tx-next request)
                                          ;;tx-next (:tx-next request)
                                          ;]
                                          [tx (:tx-next request) (db/get-ds-next)]

                                          (try
                                            (let [p (println "pure-handler >> 1")
                                                  ;tx tx
                                                  response (->> (spy tx)
                                                                (assoc request :tx-next)
                                                                pure-handler)]

                                              (when (spy (:graphql-error response))
                                                (println ">oo> ??? graphql2 when")
                                                (warn ">oo> Rolling back transaction because of graphql error: " response)
                                                (.rollback tx))
                                              response)

                                            (catch Throwable th
                                              (warn ">oo> Rolling back transaction because of " th)
                                              (println ">oo> ??? graphql1 catch")

                                              ;(jdbco/db-set-rollback-only! tx)
                                              (.rollback tx)
                                              (throw th))
                                            ))
      (pure-handler request))
    ))


;(jdbco/with-db-transaction
;  [tx (:tx request)
;   ]
;  (try (let [
;             p (println "pure-handler >> 1")
;             response (->> tx
;                           (assoc request :tx)
;                           pure-handler)]
;         (when (spy (:graphql-error response))
;           (println ">>> ??? graphql2 when")
;           (warn "Rolling back transaction because of graphql error: " response)
;           (jdbco/db-set-rollback-only! tx))
;         response)
;
;       (catch Throwable th
;         (warn "Rolling back transaction because of " th)
;
;         (println ">>> ??? graphql1 catch")
;
;         (jdbco/db-set-rollback-only! tx)
;         (throw th))))







(defn init []
  (core-graphql/init-schema! (load-schema!)))
