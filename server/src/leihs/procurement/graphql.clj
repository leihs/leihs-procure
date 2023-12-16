(ns leihs.procurement.graphql
  (:require
    [clojure.edn :as edn]

    (java.sql.Date)
    [clojure.data.json :as json]

    [clojure.java [io :as io] [jdbc :as jdbc]]
    [com.walmartlabs.lacinia :as lacinia]
    [com.walmartlabs.lacinia [parser :as graphql-parser]
     [schema :as graphql-schema] [util :as graphql-util]]
    [leihs.core.graphql :as core-graphql]
    [leihs.procurement.graphql.resolver :as resolver]
    [leihs.procurement.graphql.helpers :as helpers]
    [leihs.procurement.utils.ring-exception :refer [get-cause]]

    [leihs.procurement.authorization :refer [myp]]

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


  (spy query-string)
  (spy (-> request
           :body
           :variables))
  (spy {:request request})

  ;(println "\n>>>exec-query::graphql-query" query-string)

  ;; TODO FIXME
  (lacinia/execute (core-graphql/schema)
                   query-string
                   (-> request
                       :body
                       :variables)
                   {:request request}))

(defn pure-handler
  [{{query :query} :body, :as request}]
  (let [result (exec-query query request)
        p (println "\n>request-grapql _> request" request)
        ; (java.sql.Date)
        p (println "\n>request-c-grapql _> request (json)" (json/write-str (dissoc request :tx-next :tx :async-channel :options :handler :graphql-schema)))
        p (println "\n>request-c-grapql _> query" query)
        p (println "\n>request-grapql _> result =>" result)

        resp {:body result}]


    (if (:errors (spy result))
      (do (debug result)
          (assoc resp :graphql-error true))
      resp)

    ;(check-string-contains query "RequestsIndexFiltered")
    ;(check-string-contains query "RequestFilters")


    ;(cond
    ;  ;(and (.contains query "RequestsIndexFiltered") (:errors (result))) {:body result :status 502 :data [{:foo "servus"}]}
    ;  ;(and (.contains query "RequestsIndexFiltered") (:errors (result))) {:body result :status 200 :data [{:foo "servus"}]}
    ;  ;(and (.contains query "RequestsIndexFiltered") (:errors (result))) {:body result}
    ;
    ;  (.contains query "RequestsIndexFiltered") {:body result :status 202}
    ;  (.contains query "RequestFilters") {:body result :status 409 :message "MR/DEV-PROCUREshould not be handled"}
    ;
    ;  :else resp
    ;  )

    ))

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
      (jdbc/with-db-transaction
        [tx (:tx request)]
        (try (let [response (->> tx
                                 (assoc request :tx)
                                 pure-handler)]
               (when (spy (:graphql-error response))
                 (warn "Rolling back transaction because of graphql error: " response)
                 (jdbc/db-set-rollback-only! tx))
               response)
             (catch Throwable th
               (warn "Rolling back transaction because of " th)
               (jdbc/db-set-rollback-only! tx)
               (throw th))))
      (pure-handler request))))


(defn init []
  (core-graphql/init-schema! (load-schema!)))
