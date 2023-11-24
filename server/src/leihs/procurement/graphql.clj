(ns leihs.procurement.graphql
  (:require
    [clojure.edn :as edn]

    [clojure.java [io :as io] [jdbc :as jdbco]]
    [clojure.java [io :as io]]

    [honey.sql :refer [format] :rename {format sql-format}]
    [leihs.core.db :as db]
    [next.jdbc :as jdbc]


    [leihs.procurement.authorization :refer [myp]]

    [com.walmartlabs.lacinia :as lacinia]
    [com.walmartlabs.lacinia [parser :as graphql-parser]
     [schema :as graphql-schema] [util :as graphql-util]]
    [leihs.core.graphql :as core-graphql]
    [leihs.procurement.graphql.resolver :as resolver]
    [leihs.procurement.graphql.helpers :as helpers]
    [leihs.procurement.utils.ring-exception :refer [get-cause]]
    [taoensso.timbre :refer [debug info warn error spy]]
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

  ;(throw "my-error")

  ;(debug "graphql query" query-string
  ;       "with variables" (-> request
  ;                            :body
  ;                            :variables))


  (println "\n>>>exec-query::variables" (-> request
                                 :body
                                 :variables))

  ;(println "\n>>>exec-query::graphql-query" query-string)

  ;; FIXME
  (lacinia/execute (core-graphql/schema)
                   (spy query-string)
                   (spy (-> request
                       :body
                       :variables))
                   (spy {:request request})))

(defn pure-handler
  [{{query :query} :body, :as request}]
  ;(let [result (spy(exec-query query request))
  (let [result (exec-query query request)
        p   (println "\n>>>pure-handler" result)
        ;p   (println "\n>>>pure-handler::query" query)
        ;p   (println "\n>>>pure-handler" result)
        resp {:body result}]
    (if (:errors (spy result))
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
      (jdbco/with-db-transaction
        ;[tx (:tx-next (spy request))
        [tx (:tx-next request)

         ;tx-next (:tx-next request)
         ]
        (try (let [
                   p (println "pure-handler >> 1")
                   response (->> tx
                                 (assoc request :tx-next)
                                 pure-handler)]
               (when (:graphql-error response)
                 (warn "Rolling back transaction because of graphql error: " response)
                 (jdbco/db-set-rollback-only! tx))
               response)
             (catch Throwable th
               (warn "Rolling back transaction because of " th)
               (jdbco/db-set-rollback-only! tx)
               (throw th))))
      (let [p (println "pure-handler >> 2")
            ]
        (pure-handler request))

      )))


(defn init []
  (core-graphql/init-schema! (load-schema!)))
