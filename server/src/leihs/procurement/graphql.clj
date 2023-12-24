(ns leihs.procurement.graphql
  (:require
    [clojure.edn :as edn]

    (java.sql.Date)
    (clojure.java [io :as io] [jdbc :as jdbco])
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

  (println ">o> ??????  pure-handler 2a" query)
  (println ">o> ??????  pure-handler 2b" request)
  (println ">o> ??????  pure-handler 2c" (class request))

  (let [result (exec-query query request)
        p (println ">o> ??????  pure-handler 2d" result)
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

  (println ">o> ???? handler a")

  (let [mutation? (spy (->> query
                            (parse-query-with-exception-handling (core-graphql/schema))
                            graphql-parser/operations
                            :type
                            (= :mutation)))]
    (if (spy mutation?)



      (jdbc/with-transaction+options [tx-next (db/get-ds-next)]
                                     (letfn [(rollback-both-tx! []
                                               ;(jdbc/db-set-rollback-only! tx)
                                               (.rollback (:connectable tx-next)))]
                                       (try (let [resp (-> request
                                                           ;(assoc :tx tx)
                                                           (assoc :tx-next tx-next)
                                                           pure-handler)
                                                  resp-body (:body resp)
                                                  resp-status (:status resp)
                                                  p (println "pure-handler after request >> 1a")
                                                  ]


                                              (println ">>>>>>>>>>>?????????? response=" resp)

                                              (when (spy (:graphql-error resp))
                                                (warn ">oo> Rolling back transaction because of graphql error: " resp)
                                                (println ">oo> ??? graphql2 when")

                                                ;(.rollback (:connectable tx)))

                                                (rollback-both-tx!))

                                              resp)
                                            (catch Throwable th

                                              (println ">oo> ??? graphql1 catch")
                                              (warn "Rolling back transaction because of " (.getMessage th))
                                              (rollback-both-tx!)
                                              (throw th)))))

      (pure-handler request))
    ))


(defn init []
  (core-graphql/init-schema! (load-schema!)))
