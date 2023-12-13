(ns leihs.procurement.utils.helpers
  (:require [clojure.set :refer [subset?]]

            [taoensso.timbre :refer [debug info warn error spy]]

            ))

(defn submap? [m1 m2] (subset? (set m1) (set m2)))

(defn reject-keys [m ks] (reduce #(dissoc %1 %2) m ks))


; [leihs.procurement.utils.helpers :refer my-cast]
(defn my-cast [data]
  (println ">o> utils.helpers / my-cast " data)
  (let [
        data (if (contains? data :id)
               (assoc data :id [[:cast (:id data) :uuid]])
               data
               )

        data (if (contains? data :category_id)
               (assoc data :category_id [[:cast (:category_id data) :uuid]])
               data
               )
        data (if (contains? data :template_id)
               (assoc data :template_id [[:cast (:template_id data) :uuid]])
               data
               )

        data (if (contains? data :room_id)
               (assoc data :room_id [[:cast (:room_id data) :uuid]])
               data
               )

        data (if (contains? data :order_status)
               (assoc data :order_status [[:cast (:order_status data) :order_status_enum]])
               data
               )

        data (if (contains? data :budget_period_id)
               (assoc data :budget_period_id [[:cast (:budget_period_id data) :uuid]])
               data
               )

        data (if (contains? data :user_id)
               (assoc data :user_id [[:cast (:user_id data) :uuid]])
               data
               )

        data (if (contains? data :request_id)
               (assoc data :request_id [[:cast (:request_id data) :uuid]])
               data
               )

        data (if (contains? data :category_id)
               (assoc data :category_id [[:cast (:category_id data) :uuid]])
               data
               )

        data (if (contains? data :main_category_id)
               (assoc data :main_category_id [[:cast (:main_category_id data) :uuid]])
               data
               )

        data (if (contains? data :inspection_start_date)
               (assoc data :inspection_start_date [[:cast (:inspection_start_date data) :timestamp]])
               data
               )

        data (if (contains? data :end_date)
               (assoc data :end_date [[:cast (:end_date data) :timestamp]])
               data
               )

        data (if (contains? data :metadata)
               (do
                 (println ">o> upload::metadata")
                 (assoc data :metadata [[:cast (:metadata data) :jsonb]]) ;; works as local-test
                 ;(assoc data :metadata [[:cast (:metadata data) :json]])
                 ;(assoc data :metadata [[:cast (:metadata data) :text]]))
                 )
               data
               )

        ;[[:cast (to-name-and-lower-case a) :order_status_enum]]

        ]
    (spy data)
    )

  )