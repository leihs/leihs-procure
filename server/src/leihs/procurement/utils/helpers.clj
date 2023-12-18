(ns leihs.procurement.utils.helpers
  (:require [clojure.set :refer [subset?]]

            [taoensso.timbre :refer [debug info warn error spy]]

            ))

(defn submap? [m1 m2] (subset? (set m1) (set m2)))

(defn reject-keys [m ks] (reduce #(dissoc %1 %2) m ks))



; [leihs.procurement.utils.helpers :refer [add-comment-to-sql-format]]
(defn add-comment-to-sql-format "helper for debugging sql"
  ([sql-formatted]
   (let [
         first-element (str (first sql-formatted) (str " /* comment */"))
         ]
     (cons first-element (rest sql-formatted))))

  ([sql-format comment]
   (let [
         first-element (str (first sql-format) (str " /*" comment "*/"))
         ]
     (cons first-element (rest sql-format))))
  )

(comment
  (let [
        tx (db/get-ds-next)

        ;; examples to trigger errors
        user-id "e7ac5011-fd0e-4838-9a0f-b7da5783eede"
        ;user-id nil
        user-id ""

        x (-> (sql/select :* [true :debug-comment1])
              (sql/from :users)
              ;(sql/where [:= :id [:cast user-id :uuid]] [:= :firstname "Procurement"])
              (sql/where [:= :id [:cast user-id :uuid]])
              sql-format
              )

        p (println ">o> abc>>>>aa" x)
        ;p (println ">o> abc>>>>" (jdbc/execute-one! tx x))

        ;x (conj x "/*now-comment*/")

        p (println "\n")

        ;x (add-comment-to-sql-format x)
        x (add-comment-to-sql-format x "servus du")

        p (println ">o> abc>>>>a" x)
        p (println ">o> abc>>>>b" (jdbc/execute-one! tx (spy x)))

        ]
    )
  )

; [leihs.procurement.utils.helpers :refer [cast-ids-to-uuid]]
(defn cast-ids-to-uuid [ids]
  (map #(java.util.UUID/fromString %) ids))


(defn cast-uuids [uuids]
  (let [
        p (println ">o> uuids-sql" (class uuids))
        uuids-sql (map (fn [uuid-str] [:cast uuid-str :uuid]) (set uuids))
        p (println ">o> uuids-sql" uuids-sql)
        ]
    (spy uuids-sql)
    )
  ;(spy (map (fn [uuid-str] [:cast uuid-str :uuid]) (set uuids)))
  )


; [leihs.procurement.utils.helpers :refer [my-cast]]
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

        data (if (contains? data :main_category_id)
               (assoc data :main_category_id [[:cast (:main_category_id data) :uuid]])
               data
               )

        data (if (contains? data :inspection_start_date)
               (assoc data :inspection_start_date [[:cast (:inspection_start_date data) :timestamptz]])
               data
               )

        data (if (contains? data :end_date)
               (assoc data :end_date [[:cast (:end_date data) :timestamptz]])
               data
               )

        data (if (contains? data :metadata)
               (do
                 (println ">o> upload::metadata")
                 (assoc data :metadata [[:cast (:metadata data) :jsonb]]) ;; works as local-test
                 )
               data
               )

        data (if (contains? data :meta_data)
               (do
                 (println ">o> upload::metadata")
                 (assoc data :meta_data [[:cast (:meta_data data) :jsonb]]) ;; works as local-test
                 )
               data
               )
        ]
    (spy data)
    )

  )

