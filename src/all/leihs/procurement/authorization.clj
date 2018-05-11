(ns leihs.procurement.authorization
  (:require [clj-logging-config.log4j :as logging-config]
            [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log]
            [leihs.procurement.resources.user :as user]
            [leihs.procurement.utils.sql :as sql]
            [logbug.debug :as debug])
  (:import [leihs.procurement UnauthorizedException]))

(defn ensure-one-of
  [resolver predicates]
  (fn [context args value]
    (let [request (:request context)
          tx (:tx request)
          auth-user (:authenticated-entity request)]
      (if (->> predicates
               (map #(% tx auth-user))
               (some true?))
        (resolver context args value)
        (throw (UnauthorizedException. "Not authorized for this query path."
                                       {}))))))
