(ns leihs.procurement.utils.ring-exception
  (:refer-clojure :exclude [str keyword])
  (:require [clojure.tools.logging :as logging]
            [leihs.procurement.graphql.helpers :as helpers]
            [logbug.thrown :as thrown]))

(defn get-cause
  [e]
  (try (if (instance? java.sql.BatchUpdateException e)
         (if-let [n (.getNextException e)]
           (get-cause n)
           e)
         (if-let [c (.getCause e)]
           (get-cause c)
           e))
       (catch Throwable _ e)))

(defn wrap
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch Throwable _e
        (let [e (get-cause _e)]
          (logging/warn (thrown/to-string e))
          (logging/debug e)


          ;========= remove me
          ;   (println "error" e)
          ;   (println "error2" _e)

          (cond
            (and (instance? clojure.lang.ExceptionInfo e)
                 (contains? (ex-data e) :status))
              {:status (:status (ex-data e)),
               :body (helpers/error-as-graphql "API_ERROR" (.getMessage e))}
            (instance? org.postgresql.util.PSQLException e)
              {:status 409,
               :body (helpers/error-as-graphql "DATABASE_ERROR"
                                               (.getMessage e))}
            :else
              {:status 500,
               :body
                 (helpers/error-as-graphql
                   "UNKNOWN_SERVER_ERROR"
                   "Unclassified error, see the server logs for details.")}))))))
