(ns leihs.procurement.permissions.request-helpers
  (:require [leihs.procurement.permissions.request-fields :as
             request-fields-perms]))

(def attrs-to-exclude #{:id :state :total_price_cents})

(defn- fallback-p-spec [value] {:value value, :read true, :write true})

(defn with-protected-value
  [p-spec value]
  (->> value
       (if (:read p-spec))
       (assoc p-spec :value)))

(defn value-with-permissions
  [field-perms transform-fn attr value]
  (if (attrs-to-exclude attr)
    {attr value}
    {attr (let [res (if-let [p-spec (attr field-perms)]
                      (with-protected-value p-spec value)
                      ; FIXME: this is a general whitelist fallback
                      ; remove when all field permissions implemented
                      (fallback-p-spec value))]
            (transform-fn res))}))

(defn apply-permissions
  ([tx auth-user proc-request]
   (apply-permissions tx auth-user proc-request identity))
  ([tx auth-user proc-request transform-fn]
   (let [field-perms (request-fields-perms/get-for-user-and-request
                       tx
                       auth-user
                       proc-request)]
     (->> proc-request
          (map #(apply value-with-permissions field-perms transform-fn %))
          (into {})))))

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
