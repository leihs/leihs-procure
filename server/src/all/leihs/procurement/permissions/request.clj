(ns leihs.procurement.permissions.request
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log]
            [leihs.procurement.permissions.request-fields :as
             request-fields-perms]
            [leihs.procurement.utils.ds :refer [get-ds]]))

(def attrs-to-exclude #{:id})

(defn apply-permissions
  [tx auth-user proc-request]
  (let [field-perms (request-fields-perms/get-for-user-and-request
                      tx
                      auth-user
                      proc-request)]
    (into {}
          (map
            (fn [[attr value]]
              (if (attrs-to-exclude attr)
                {attr value}
                {attr (if-let [p-spec (attr field-perms)]
                        (assoc p-spec :value (if (:read p-spec) value))
                        ; FIXME: this is a general whitelist fallback
                        ; remove when all field permissions implemented
                        {:value value, :read true, :write true})}))
            proc-request))))

(defn authorized-to-write-all-fields?
  ([tx auth-user write-data]
   "For creating new request"
   (authorized-to-write-all-fields? tx auth-user write-data write-data))
  ([tx auth-user request write-data]
   "For updating an existing request"
   (let [request-data-with-perms (apply-permissions tx auth-user request)]
     (->> write-data
          (map first)
          (map #(% request-data-with-perms))
          (map :write)
          (every? true?)))))
