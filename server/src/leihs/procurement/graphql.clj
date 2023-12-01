(ns leihs.procurement.graphql
  (:require
    [clojure.edn :as edn]

    (clojure.java [io :as io] [jdbc :as jdbco])
    (clojure.java [io :as io])

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






(defn str-to-uuid [s]
  (java.util.UUID/fromString s))

(def keys-to-cast [:budgetPeriods :organizations :categories])




;(defn convert-uuids [m]
;  (-> m
;      (update :organizations #(map str-to-uuid %))
;      (update :categories #(map str-to-uuid %))
;      (update :budgetPeriods #(map str-to-uuid %))))

;(defn update-uuid-keys [m keys]
;  (reduce (fn [acc key]
;            (update acc key #(map str-to-uuid %)))
;          m
;          keys))


(defn cast-keys-to-uuids [m keys-to-cast]
  (reduce (fn [acc key]
            (if (contains? m key)
              (update acc key (fn [v] (mapv #(java.util.UUID/fromString %) v)))
              acc))
          m
          keys-to-cast))


(defn exec-query
  [query-string request]
  (debug "graphql query" query-string
         "with variables" (-> request
                              :body
                              :variables))
  (let [
        vars (-> request
                 :body
                 :variables)
        order-status (get-in request [:body :variables :order_status])
        priority (get-in request [:body :variables :priority])
        inspector_priority (get-in request [:body :variables :inspector_priority])

        p (println ">oo> vars from graphQL-Requst" order-status priority inspector_priority vars)
        ])


  (lacinia/execute (core-graphql/schema)
                   query-string
                   (-> request
                       :body
                       :variables)
                   {:request request}))


(defn pure-handler
  [{{query :query} :body, :as request}]
  ;(let [result (spy(exec-query query request))
  (let [result (exec-query query request)
        ;p (println "\n>oo>1pure-handler _> request, can contains invalid value in -> priority inspector_priority order_status")
        ;p (println "\n>o>1pure-handler _> request" request)
        ;p (println "\n>o>1pure-handler _> request (json)" (json/write-str request))

        p (println "\n>oo>1pure-handler _> request" request)
        p (println "\n>o>2pure-handler _> query" query)
        p (println "\n>o>3pure-handler, result=>" result)

        ;p (println "\n>oo>2pure-handler _> query" query)
        ;p (println "\n>o>3pure-handler, result=>" result)
        resp {:body result}]


    (if (:errors (spy result))
      (do (debug result)
          (assoc resp :graphql-error true)
          (println "\n>o> ERROR 4pure-handler, result=>" result)
          )
      (do
        ;(println "\n>o>3apure-handler, result=>" result)
        resp))

    resp

    ;(check-string-contains query "RequestsIndexFiltered")
    ;(check-string-contains query "RequestFilters")


    ;(cond
    ;  ;(and (.contains query "RequestsIndexFiltered") (:errors (result))) {:body result :status 502 :data [{:foo "servus"}]}
    ;  ;(and (.contains query "RequestsIndexFiltered") (:errors (result))) {:body result :status 200 :data [{:foo "servus"}]}
    ;  ;(and (.contains query "RequestsIndexFiltered") (:errors (result))) {:body result}
    ;  ;(.contains query "RequestsIndexFiltered") {:body result}
    ;  ;(.contains query "RequestFilters") {:body result :status 409 :message "should not be handled"}
    ;  (.contains query "RequestFilters")  (throw "my-error")
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
