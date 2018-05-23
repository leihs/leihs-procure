(ns leihs.procurement.permissions.request
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log]
            [leihs.procurement.permissions.request-fields :as
             request-fields-perms]
            [leihs.procurement.utils.ds :refer [get-ds]]))

(defn apply-permissions
  [tx auth-user proc-request]
  (let [field-perms (request-fields-perms/get-for-user-and-request
                      tx
                      auth-user
                      proc-request)]
    (into {}
          (map (fn [[attr value]]
                 {attr (if-let [p-spec (attr field-perms)]
                         (and (:read p-spec) (assoc p-spec :value value))
                         value)})
            proc-request))))

(defn authorized-to-write-all-fields?
  ([tx auth-user request]
   (authorized-to-write-all-fields? tx auth-user request request))
  ([tx auth-user request write-data]
   (let [request-data-with-perms (apply-permissions tx auth-user request)]
     (->> write-data
          (map first)
          (filter #(not (% request-fields-perms/field-exceptions)))
          (map #(% request-data-with-perms))
          (map :write)
          (every? true?)))))

; (apply-permissions (get-ds) {:id "2c78414f-b9ad-5b89-8b93-3d651566eb8e"}
; {:article_name "test"})
; (authorized-to-write-all-fields?
;   (get-ds)
;   {:id "2c78414f-b9ad-5b89-8b93-3d651566eb8e"}
;   {:article_name "test"})
