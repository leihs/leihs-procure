(ns leihs.procurement.permissions.request
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log]
            [leihs.procurement.permissions.request-fields :as
             request-fields-perms]))

(defn apply-permissions
  [tx auth-user proc-request]
  (let [proc-request-perms (request-fields-perms/get-for-user-and-request
                             tx
                             auth-user
                             proc-request)]
    (into {}
          (map (fn [[attr value]]
                 {attr (if-let [p-spec (attr proc-request-perms)]
                         (and (:read p-spec) (assoc p-spec :value value))
                         value)})
            proc-request))))

(defn authorized-to-write-all-fields?
  [tx auth-user request]
  (->> request
       (apply-permissions tx auth-user)
       (map second)
       (map :write)
       (every? true?)))
