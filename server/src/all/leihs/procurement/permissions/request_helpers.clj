(ns leihs.procurement.permissions.request-helpers
  (:require [clojure.tools.logging :as log]
            [leihs.procurement.permissions.request-fields :as
             request-fields-perms]))

(def attrs-to-skip #{:id :state :total_price_cents})

(def special-perms #{:DELETE})

(defn- include-special-perms
  [field-perms req-vec]
  (reduce (fn [acc el] (conj acc [el (el field-perms)])) req-vec special-perms))

(defn- fallback-p-spec [value] {:value value, :read true, :write true})

(defn with-protected-value
  [p-spec value]
  (->> value
       (if (:read p-spec))
       (assoc p-spec :value)))

(defn value-with-permissions
  [field-perms transform-fn attr value]
  (if (attrs-to-skip attr)
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
          (include-special-perms field-perms)
          (into {})))))

(defn authorized-to-write-all-fields?
  ([tx auth-user write-data]
   "For creating new request"
   (authorized-to-write-all-fields? tx auth-user write-data write-data))
  ([tx auth-user request write-data]
   "For updating an existing request"
   (let [request* (cond-> request
                    (not (:user request)) (assoc :user (:user_id auth-user)))
         request-data-with-perms (apply-permissions tx auth-user request*)]
     (->> write-data
          (map first)
          (map #(% request-data-with-perms))
          (map :write)
          (every? true?)))))

; NOTE: `edit` action means only editing the *form* fields!
(def non-form-fields
  [:budget_period :category :created_at :organization :updated_at]); TODO: why are the timestamps writable tho?

(defn add-action-permissions
  [req]
  (let [can-edit-any-fields? (->> (apply dissoc req non-form-fields)
                                  (map (fn [[k v]] (:write v)))
                                  (filter true?)
                                  empty?
                                  not)
        can-change-budget-period? (-> req
                                      :budget_period
                                      :write)
        can-change-category? (-> req
                                 :category
                                 :write)
        can-delete? (:DELETE req)]
    (assoc req
      :actionPermissions {:edit can-edit-any-fields?,
                          :delete can-delete?,
                          :moveBudgetPeriod can-change-budget-period?,
                          :moveCategory can-change-category?})))
