(ns leihs.procurement.graphql.resolver
  (:require
    [clojure.tools.logging :as log]
    [com.walmartlabs.lacinia.resolve :as graphql-resolve]

    [taoensso.timbre :refer [debug info warn error spy]]


    [leihs.procurement.graphql [mutations :as mutations]
     [queries :as queries]]
    [leihs.procurement.utils.ring-exception :refer [get-cause]]))

(defn wrap-resolver-with-error
  [resolver]

  ;(println ">o> wrap-resolver-with-error ???" resolver)

  (fn [context args value]
    (try (spy ((spy resolver) (spy context) (spy args) (spy value)))
         (catch Throwable e*
           (let [e (get-cause e*)
                 p (println ">o> within catch ")

                 ;p (println ">e>" e)                        ;; TODO: used to print full exception

                 m (.getMessage e)
                 n (-> e*
                       .getClass
                       .getSimpleName)]
             (log/warn (or m n))
             (log/debug e)
             (spy (graphql-resolve/resolve-as nil
                                         {:message (str m),
                                          ; if message nil
                                          ; convert to ""
                                          :exception n})

                  ))))))

(defn- wrap-map-with-error
  [arg]
  (into {} (for [[k v] (spy arg)] [k (wrap-resolver-with-error v)])))

(def resolvers
  (-> queries/resolvers
      (merge mutations/resolvers)
      wrap-map-with-error))
