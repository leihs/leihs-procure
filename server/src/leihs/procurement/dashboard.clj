(ns leihs.procurement.dashboard
  (:require [clojure.string :as string]
            [clojure.data.json :as json]

            [taoensso.timbre :refer [debug info warn error spy]]

            [leihs.procurement.utils.helpers :refer [cast-uuids]]
            [clojure.data.json :as json]
    ;[clojure.java.jdbc :as jdbc]
    ;[leihs.procurement.utils.sql :as sql]

            [logbug.debug :as debug]


            [honey.sql :refer [format] :rename {format sql-format}]
            [leihs.core.db :as db]
            [next.jdbc :as jdbc]
            [honey.sql.helpers :as sql]

            [clojure.tools.logging :as log]
            [leihs.procurement.resources.budget-periods :as budget-periods]
            [leihs.procurement.resources.categories :as categories]
            [leihs.procurement.resources.main-categories :as main-categories]
            [leihs.procurement.resources.requests :as requests]))

(defn sum-total-price
  [coll]
  (println ">o> sum-total-price ???" (->> coll
                                          (map :total_price_cents)))

  (println ">o> sum-total-price ???" (->> coll
                                          (map :name)))

  (spy (->> coll
            (map :total_price_cents)
            (reduce +))))



;(comment
;
;  (let [
;        ss [{:total_price_cents 1000}
;            {:total_price_cents 2000}
;            {:total_price_cents nil}
;            {:total_price_cents 1500}]
;
;        ;ss [{}]
;
;        p (println ">oo>" (sum-total-price ss))
;
;        ])
;  )

(defn cache-key
  [& args]
  (->> args
       (map :id)
       (string/join "_")))

(defn create-budget [bps tx requests dashboard-cache-key main-cats]
  (->>
    bps
    (map (fn [bp]
           (let [

                 main-cats* (->> main-cats
                                 (map (fn [mc]
                                        (let [
                                              p (println ">o> xxx debug mc=" (->> mc
                                                                                  (map :name)
                                                                                  ))

                                              cats* (->> mc
                                                         :id
                                                         (categories/get-for-main-category-id tx)
                                                         (map (fn [c] (let [
                                                                            p (println ">o> c ???1 xxx before filter  c=" c)
                                                                            p (println ">o> c ???1 xxx before filter bp=" bp)

                                                                            requests* (spy (filter #(and (= (-> %
                                                                                                                :category
                                                                                                                :value
                                                                                                                :id)
                                                                                                            (str (:id c)))
                                                                                                         (= (-> %
                                                                                                                :budget_period
                                                                                                                :value
                                                                                                                :id)
                                                                                                            (str (:id bp))))
                                                                                                   requests))
                                                                            p (println ">>>id here ??? xxx requests*" requests*)
                                                                            ]
                                                                        (-> c
                                                                            (assoc :requests requests*)
                                                                            (assoc :total_price_cents (sum-total-price requests*))
                                                                            (assoc :cacheKey
                                                                                   (cache-key
                                                                                     dashboard-cache-key
                                                                                     bp
                                                                                     mc
                                                                                     c)))))))

                                              p (println ">o> xxx cats*=" (->> cats*
                                                                               (map :name)
                                                                               ))



                                              merged-path (-> mc
                                                              (assoc :categories cats*)
                                                              (assoc :total_price_cents (sum-total-price cats*))
                                                              (assoc :cacheKey
                                                                     (cache-key dashboard-cache-key bp mc))
                                                              (->> (main-categories/merge-image-path
                                                                     tx)))


                                              p (println ">o> xxx merged-path=" (->> merged-path
                                                                                     (map :name)
                                                                                     ))
                                              ]
                                          merged-path

                                          ))))]
             (-> bp
                 (assoc :main_categories main-cats*)
                 (assoc :cacheKey (cache-key dashboard-cache-key bp))
                 (assoc :total_price_cents (sum-total-price main-cats*))))))))

(defn cache-key
  [& args]
  (->> args
       (map :id)
       (string/join "_")))

(defn get-dashboard
  [ctx args value]

  (println ">debug> >>> FIRST LINE > get-dashboard????")

  (let [ring-request (:request ctx)
        tx (:tx-next ring-request)
        cat-ids (:category_id args)
        bp-ids (:budget_period_id args)
        main-cats (-> main-categories/main-categories-base-query
                      sql-format
                      (->> (jdbc/execute! tx)))


        ;; DEBUG
        main-cats-query (-> main-categories/main-categories-base-query
                            sql-format)
        p (println ">>> resultA1-2a xxx >query query-bps " main-cats-query)


        ;; DEBUG
        bps-query (-> budget-periods/budget-periods-base-query
                      (cond-> bp-ids (sql/where
                                       ;[:in :procurement_budget_periods.id (cast-ids-to-uuid bp-ids)]))
                                       [:in :procurement_budget_periods.id (cast-uuids bp-ids)]))
                      sql-format
                      )

        p (println ">>> resultA1-2b xxx >query query-bps " bps-query)



        bps (if (or (not bp-ids) (not-empty bp-ids))
              (-> budget-periods/budget-periods-base-query
                  (cond-> bp-ids (sql/where
                                   ;[:in :procurement_budget_periods.id (cast-ids-to-uuid bp-ids)]))
                                   [:in :procurement_budget_periods.id (cast-uuids bp-ids)]))
                  sql-format
                  (->> (jdbc/execute! tx)))
              [])

        p (println ">>resultA1-2 xxx bps" bps)


        requests (requests/get-requests ctx args value)
        p (println ">>requestsB2 xxx" requests)
        p (println ">>requestsB2 xxx requests.count" (count requests))


        ;p (throw (Exception. "fake error"))

        dashboard-cache-key {:id (hash args)}]
    {:total_count (count requests),
     :cacheKey (cache-key dashboard-cache-key),
     :budget_periods (create-budget bps tx requests dashboard-cache-key main-cats) ;; correct result with hard-coded-data
     }))


(defn copy-requests [a] a)
(defn copy-bps [a] a)
(defn copy-dashboard-cache-key [a] a)
(defn copy-main-cats [a] a)


;; Example-2: reuse logged fnc-attributes  - start
(defn extract-log-entry "extracts params from log-entry" [entry]
  (let [func-symbol (first entry)
        args (get-in entry [2 :args])]
    [func-symbol args]))

;(debug/debug-ns *ns*)

;[logbug.debug :as debug]
;(debug/debug-ns *ns*)


;; original / test-data from master, TODO: reuse of params from logs
(def le-copy-request '[copy-requests "invoked" {:args (({:category {:read true, :write true, :default {:id #uuid "bdaaea03-8180-4a19-8709-33abef8f26d3", :name "category_1_B", :main_category_id #uuid "35aacc62-c9e9-478a-a396-3a3f00995f7b", :general_ledger_account "5762959998", :cost_center "7429400349", :procurement_account "4912540823"}, :required true, :value {:id "bdaaea03-8180-4a19-8709-33abef8f26d3", :name "category_1_B", :cost_center "7429400349", :main_category {:id "35aacc62-c9e9-478a-a396-3a3f00995f7b", :name "main_category_1"}, :main_category_id "35aacc62-c9e9-478a-a396-3a3f00995f7b", :procurement_account "4912540823", :general_ledger_account "5762959998"}, :request-id #uuid "83115fa8-c8d8-440f-9b5d-ef6aa4ad032d"}, :supplier {:read true, :write true, :default nil, :required false, :value nil, :request-id #uuid "83115fa8-c8d8-440f-9b5d-ef6aa4ad032d"}, :motivation {:read true, :write true, :required true, :value "Repudiandae voluptas dolorem mollitia.", :request-id #uuid "83115fa8-c8d8-440f-9b5d-ef6aa4ad032d"}, :accounting_type {:read false, :write false, :default "aquisition", :required true, :value nil, :request-id #uuid "83115fa8-c8d8-440f-9b5d-ef6aa4ad032d"}, :actionPermissions {:edit true, :delete true, :moveBudgetPeriod true, :moveCategory true}, :article_number {:read true, :write true, :default nil, :required false, :value nil, :request-id #uuid "83115fa8-c8d8-440f-9b5d-ef6aa4ad032d"}, :inspector_priority {:read false, :write false, :default "MEDIUM", :required true, :value nil, :request-id #uuid "83115fa8-c8d8-440f-9b5d-ef6aa4ad032d"}, :price_cents {:read true, :write true, :default 0, :required true, :value 103, :request-id #uuid "83115fa8-c8d8-440f-9b5d-ef6aa4ad032d"}, :budget_period {:read true, :write true, :default {:id #uuid "f3b0d720-562c-4158-a8b5-65a4ca84787d", :name "budget_period_I", :inspection_start_date #time/instant "2024-01-21T10:25:08.624239Z", :end_date #time/instant "2024-03-21T10:25:08.624465Z", :created_at #time/instant "2023-12-22T10:25:08.624630Z", :updated_at #time/instant "2023-12-22T10:25:08.624630Z"}, :required true, :value {:id "f3b0d720-562c-4158-a8b5-65a4ca84787d", :name "budget_period_I", :inspection_start_date "2024-01-21T11:25:08.624239+01:00", :end_date "2024-03-21T11:25:08.624465+01:00", :created_at "2023-12-22T11:25:08.62463", :updated_at "2023-12-22T11:25:08.62463", :is_past false}, :request-id #uuid "83115fa8-c8d8-440f-9b5d-ef6aa4ad032d"}, :article_name {:read true, :write true, :default nil, :required true, :value "Anaphoric Macro", :request-id #uuid "83115fa8-c8d8-440f-9b5d-ef6aa4ad032d"}, :replacement {:read true, :write true, :default nil, :required true, :value true, :request-id #uuid "83115fa8-c8d8-440f-9b5d-ef6aa4ad032d"}, :attachments {:read true, :write true, :required false, :value :unqueried, :request-id #uuid "83115fa8-c8d8-440f-9b5d-ef6aa4ad032d"}, :DELETE true, :template {:read true, :write false, :default nil, :required true, :value nil, :request-id #uuid "83115fa8-c8d8-440f-9b5d-ef6aa4ad032d"}, :state :NEW, :organization {:value {:id "834c72c5-a82f-46d3-ae84-6371c809026a", :name "Grocery & Electronics", :parent_id "1f894025-cfdc-4c0f-8a1f-6521926cafff", :shortname nil, :department {:id "1f894025-cfdc-4c0f-8a1f-6521926cafff", :name "Tools", :parent_id nil, :shortname nil}}, :read false, :write false, :request-id #uuid "83115fa8-c8d8-440f-9b5d-ef6aa4ad032d"}, :supplier_name {:read true, :write true, :default nil, :required false, :value "Gulgowski-Gulgowski", :request-id #uuid "83115fa8-c8d8-440f-9b5d-ef6aa4ad032d"}, :order_status {:read true, :write false, :default "NOT_PROCURED", :required true, :value :NOT_PROCESSED, :request-id #uuid "83115fa8-c8d8-440f-9b5d-ef6aa4ad032d"}, :requested_quantity {:read true, :write true, :required true, :value 1, :request-id #uuid "83115fa8-c8d8-440f-9b5d-ef6aa4ad032d"}, :general_ledger_account {:read false, :write false, :required false, :value nil, :request-id #uuid "83115fa8-c8d8-440f-9b5d-ef6aa4ad032d"}, :updated_at {:value #time/instant "2023-12-22T10:25:09.574805Z", :read false, :write false, :request-id #uuid "83115fa8-c8d8-440f-9b5d-ef6aa4ad032d"}, :priority {:read true, :write true, :default "NORMAL", :required true, :value :NORMAL, :request-id #uuid "83115fa8-c8d8-440f-9b5d-ef6aa4ad032d"}, :id #uuid "83115fa8-c8d8-440f-9b5d-ef6aa4ad032d", :total_price_cents 103, :price_currency {:read true, :write false, :default "CHF", :required true, :value "CHF", :request-id #uuid "83115fa8-c8d8-440f-9b5d-ef6aa4ad032d"}, :procurement_account {:read false, :write false, :value nil, :request-id #uuid "83115fa8-c8d8-440f-9b5d-ef6aa4ad032d"}, :room {:read true, :write true, :default {:id #uuid "b8927fd2-014c-48c2-b095-fa1d5620debe", :name "general room", :description nil, :building_id #uuid "abae04c5-d767-425e-acc2-7ce04df645d1", :general true, :building {:id #uuid "abae04c5-d767-425e-acc2-7ce04df645d1", :name "general building", :code nil}}, :required true, :value {:id "a8bd3f00-0517-4857-bb8c-d16ca1037b8a", :name "263", :general false, :building {:id "19999f8b-619c-46d3-a16c-d36e5883e536", :code nil, :name "997 Jessika Valley"}, :building_id "19999f8b-619c-46d3-a16c-d36e5883e536", :description nil}, :request-id #uuid "83115fa8-c8d8-440f-9b5d-ef6aa4ad032d"}, :inspection_comment {:read false, :write false, :required false, :value nil, :request-id #uuid "83115fa8-c8d8-440f-9b5d-ef6aa4ad032d"}, :approved_quantity {:read false, :write false, :required false, :value nil, :request-id #uuid "83115fa8-c8d8-440f-9b5d-ef6aa4ad032d"}, :order_quantity {:read false, :write false, :required false, :value nil, :request-id #uuid "83115fa8-c8d8-440f-9b5d-ef6aa4ad032d"}, :cost_center {:read false, :write false, :required false, :value nil, :request-id #uuid "83115fa8-c8d8-440f-9b5d-ef6aa4ad032d"}, :user {:read true, :write false, :required true, :default {:id #uuid "99a8aa98-38b4-44a7-9517-888f96788378", :firstname "Joaquin", :lastname "Volkman"}, :value {:system_admin_protected false, :address nil, :email "haywood@feeney.biz", :pool_protected false, :last_sign_in_at "2023-12-22T11:25:09.913+01:00", :img32_url nil, :account_enabled true, :lastname "Volkman", :phone nil, :img_digest nil, :org_id nil, :extended_info nil, :secondary_email nil, :city nil, :settings nil, :is_admin false, :organization "local", :login nil, :searchable "Joaquin Volkman haywood@feeney.biz    Volkman Joaquin", :updated_at "2023-12-22T11:25:09.913+01:00", :firstname "Joaquin", :zip nil, :id "99a8aa98-38b4-44a7-9517-888f96788378", :url nil, :password_sign_in_enabled true, :account_disabled_at nil, :is_system_admin false, :badge_id nil, :language_locale nil, :img256_url nil, :country nil, :delegator_user_id nil, :created_at "2023-12-22T00:00:00+01:00", :admin_protected false}, :request-id #uuid "83115fa8-c8d8-440f-9b5d-ef6aa4ad032d"}, :receiver {:read true, :write true, :required false, :value nil, :request-id #uuid "83115fa8-c8d8-440f-9b5d-ef6aa4ad032d"}, :internal_order_number {:read false, :write false, :required false, :value nil, :request-id #uuid "83115fa8-c8d8-440f-9b5d-ef6aa4ad032d"}, :created_at {:value #time/instant "2023-12-22T10:25:09.574805Z", :read false, :write false, :request-id #uuid "83115fa8-c8d8-440f-9b5d-ef6aa4ad032d"}, :order_comment {:read true, :write false, :required false, :value nil, :request-id #uuid "83115fa8-c8d8-440f-9b5d-ef6aa4ad032d"}, :short_id "budget_period_I.002", :model {:read true, :write true, :default nil, :required false, :value nil, :request-id #uuid "83115fa8-c8d8-440f-9b5d-ef6aa4ad032d"}} {:category {:read true, :write true, :default {:id #uuid "1a61554f-4758-458d-8441-fe3e20fd4b46", :name "category_1_C", :main_category_id #uuid "35aacc62-c9e9-478a-a396-3a3f00995f7b", :general_ledger_account "7981816261", :cost_center "9370700739", :procurement_account "6251253361"}, :required true, :value {:id "1a61554f-4758-458d-8441-fe3e20fd4b46", :name "category_1_C", :cost_center "9370700739", :main_category {:id "35aacc62-c9e9-478a-a396-3a3f00995f7b", :name "main_category_1"}, :main_category_id "35aacc62-c9e9-478a-a396-3a3f00995f7b", :procurement_account "6251253361", :general_ledger_account "7981816261"}, :request-id #uuid "c8262e80-3407-46ce-ac2d-b7e54968a853"}, :supplier {:read true, :write true, :default nil, :required false, :value nil, :request-id #uuid "c8262e80-3407-46ce-ac2d-b7e54968a853"}, :motivation {:read true, :write true, :required true, :value "Neque consequatur ab vel.", :request-id #uuid "c8262e80-3407-46ce-ac2d-b7e54968a853"}, :accounting_type {:read false, :write false, :default "aquisition", :required true, :value nil, :request-id #uuid "c8262e80-3407-46ce-ac2d-b7e54968a853"}, :actionPermissions {:edit true, :delete true, :moveBudgetPeriod true, :moveCategory true}, :article_number {:read true, :write true, :default nil, :required false, :value nil, :request-id #uuid "c8262e80-3407-46ce-ac2d-b7e54968a853"}, :inspector_priority {:read false, :write false, :default "MEDIUM", :required true, :value nil, :request-id #uuid "c8262e80-3407-46ce-ac2d-b7e54968a853"}, :price_cents {:read true, :write true, :default 0, :required true, :value 109, :request-id #uuid "c8262e80-3407-46ce-ac2d-b7e54968a853"}, :budget_period {:read true, :write true, :default {:id #uuid "f3b0d720-562c-4158-a8b5-65a4ca84787d", :name "budget_period_I", :inspection_start_date #time/instant "2024-01-21T10:25:08.624239Z", :end_date #time/instant "2024-03-21T10:25:08.624465Z", :created_at #time/instant "2023-12-22T10:25:08.624630Z", :updated_at #time/instant "2023-12-22T10:25:08.624630Z"}, :required true, :value {:id "f3b0d720-562c-4158-a8b5-65a4ca84787d", :name "budget_period_I", :inspection_start_date "2024-01-21T11:25:08.624239+01:00", :end_date "2024-03-21T11:25:08.624465+01:00", :created_at "2023-12-22T11:25:08.62463", :updated_at "2023-12-22T11:25:08.62463", :is_past false}, :request-id #uuid "c8262e80-3407-46ce-ac2d-b7e54968a853"}, :article_name {:read true, :write true, :default nil, :required true, :value "Heavy Duty Silk Table", :request-id #uuid "c8262e80-3407-46ce-ac2d-b7e54968a853"}, :replacement {:read true, :write true, :default nil, :required true, :value true, :request-id #uuid "c8262e80-3407-46ce-ac2d-b7e54968a853"}, :attachments {:read true, :write true, :required false, :value :unqueried, :request-id #uuid "c8262e80-3407-46ce-ac2d-b7e54968a853"}, :DELETE true, :template {:read true, :write false, :default nil, :required true, :value nil, :request-id #uuid "c8262e80-3407-46ce-ac2d-b7e54968a853"}, :state :NEW, :organization {:value {:id "cc82ab6e-06d1-450d-a29c-bd1c664db928", :name "Home", :parent_id "dd493c5c-69a9-4690-a915-71611f6ef987", :shortname nil, :department {:id "dd493c5c-69a9-4690-a915-71611f6ef987", :name "Grocery", :parent_id nil, :shortname nil}}, :read false, :write false, :request-id #uuid "c8262e80-3407-46ce-ac2d-b7e54968a853"}, :supplier_name {:read true, :write true, :default nil, :required false, :value "Ritchie-Will", :request-id #uuid "c8262e80-3407-46ce-ac2d-b7e54968a853"}, :order_status {:read true, :write false, :default "NOT_PROCURED", :required true, :value :NOT_PROCESSED, :request-id #uuid "c8262e80-3407-46ce-ac2d-b7e54968a853"}, :requested_quantity {:read true, :write true, :required true, :value 1, :request-id #uuid "c8262e80-3407-46ce-ac2d-b7e54968a853"}, :general_ledger_account {:read false, :write false, :required false, :value nil, :request-id #uuid "c8262e80-3407-46ce-ac2d-b7e54968a853"}, :updated_at {:value #time/instant "2023-12-22T10:25:09.589181Z", :read false, :write false, :request-id #uuid "c8262e80-3407-46ce-ac2d-b7e54968a853"}, :priority {:read true, :write true, :default "NORMAL", :required true, :value :NORMAL, :request-id #uuid "c8262e80-3407-46ce-ac2d-b7e54968a853"}, :id #uuid "c8262e80-3407-46ce-ac2d-b7e54968a853", :total_price_cents 109, :price_currency {:read true, :write false, :default "CHF", :required true, :value "CHF", :request-id #uuid "c8262e80-3407-46ce-ac2d-b7e54968a853"}, :procurement_account {:read false, :write false, :value nil, :request-id #uuid "c8262e80-3407-46ce-ac2d-b7e54968a853"}, :room {:read true, :write true, :default {:id #uuid "b8927fd2-014c-48c2-b095-fa1d5620debe", :name "general room", :description nil, :building_id #uuid "abae04c5-d767-425e-acc2-7ce04df645d1", :general true, :building {:id #uuid "abae04c5-d767-425e-acc2-7ce04df645d1", :name "general building", :code nil}}, :required true, :value {:id "819357a4-f114-4abf-b3e4-f7f6e8fe4bd1", :name "5720", :general false, :building {:id "292471f2-3784-49b6-936f-ba3585c63583", :code nil, :name "305 Wisozk Walk"}, :building_id "292471f2-3784-49b6-936f-ba3585c63583", :description nil}, :request-id #uuid "c8262e80-3407-46ce-ac2d-b7e54968a853"}, :inspection_comment {:read false, :write false, :required false, :value nil, :request-id #uuid "c8262e80-3407-46ce-ac2d-b7e54968a853"}, :approved_quantity {:read false, :write false, :required false, :value nil, :request-id #uuid "c8262e80-3407-46ce-ac2d-b7e54968a853"}, :order_quantity {:read false, :write false, :required false, :value nil, :request-id #uuid "c8262e80-3407-46ce-ac2d-b7e54968a853"}, :cost_center {:read false, :write false, :required false, :value nil, :request-id #uuid "c8262e80-3407-46ce-ac2d-b7e54968a853"}, :user {:read true, :write false, :required true, :default {:id #uuid "99a8aa98-38b4-44a7-9517-888f96788378", :firstname "Joaquin", :lastname "Volkman"}, :value {:system_admin_protected false, :address nil, :email "haywood@feeney.biz", :pool_protected false, :last_sign_in_at "2023-12-22T11:25:09.913+01:00", :img32_url nil, :account_enabled true, :lastname "Volkman", :phone nil, :img_digest nil, :org_id nil, :extended_info nil, :secondary_email nil, :city nil, :settings nil, :is_admin false, :organization "local", :login nil, :searchable "Joaquin Volkman haywood@feeney.biz    Volkman Joaquin", :updated_at "2023-12-22T11:25:09.913+01:00", :firstname "Joaquin", :zip nil, :id "99a8aa98-38b4-44a7-9517-888f96788378", :url nil, :password_sign_in_enabled true, :account_disabled_at nil, :is_system_admin false, :badge_id nil, :language_locale nil, :img256_url nil, :country nil, :delegator_user_id nil, :created_at "2023-12-22T00:00:00+01:00", :admin_protected false}, :request-id #uuid "c8262e80-3407-46ce-ac2d-b7e54968a853"}, :receiver {:read true, :write true, :required false, :value nil, :request-id #uuid "c8262e80-3407-46ce-ac2d-b7e54968a853"}, :internal_order_number {:read false, :write false, :required false, :value nil, :request-id #uuid "c8262e80-3407-46ce-ac2d-b7e54968a853"}, :created_at {:value #time/instant "2023-12-22T10:25:09.589181Z", :read false, :write false, :request-id #uuid "c8262e80-3407-46ce-ac2d-b7e54968a853"}, :order_comment {:read true, :write false, :required false, :value nil, :request-id #uuid "c8262e80-3407-46ce-ac2d-b7e54968a853"}, :short_id "budget_period_I.004", :model {:read true, :write true, :default nil, :required false, :value nil, :request-id #uuid "c8262e80-3407-46ce-ac2d-b7e54968a853"}} {:category {:read true, :write true, :default {:id #uuid "a22c4042-03f7-4636-8296-448af710d4be", :name "category_2_A", :main_category_id #uuid "98e969ed-e6c1-4205-956e-ed7508a47fe5", :general_ledger_account "3517987317", :cost_center "4793874989", :procurement_account "6603351852"}, :required true, :value {:id "a22c4042-03f7-4636-8296-448af710d4be", :name "category_2_A", :cost_center "4793874989", :main_category {:id "98e969ed-e6c1-4205-956e-ed7508a47fe5", :name "main_category_2"}, :main_category_id "98e969ed-e6c1-4205-956e-ed7508a47fe5", :procurement_account "6603351852", :general_ledger_account "3517987317"}, :request-id #uuid "fce96619-552e-4525-a0bb-b2cf1565cba7"}, :supplier {:read true, :write true, :default nil, :required false, :value nil, :request-id #uuid "fce96619-552e-4525-a0bb-b2cf1565cba7"}, :motivation {:read true, :write true, :required true, :value "Reiciendis nisi porro voluptate.", :request-id #uuid "fce96619-552e-4525-a0bb-b2cf1565cba7"}, :accounting_type {:read false, :write false, :default "aquisition", :required true, :value nil, :request-id #uuid "fce96619-552e-4525-a0bb-b2cf1565cba7"}, :actionPermissions {:edit true, :delete true, :moveBudgetPeriod true, :moveCategory true}, :article_number {:read true, :write true, :default nil, :required false, :value nil, :request-id #uuid "fce96619-552e-4525-a0bb-b2cf1565cba7"}, :inspector_priority {:read false, :write false, :default "MEDIUM", :required true, :value nil, :request-id #uuid "fce96619-552e-4525-a0bb-b2cf1565cba7"}, :price_cents {:read true, :write true, :default 0, :required true, :value 113, :request-id #uuid "fce96619-552e-4525-a0bb-b2cf1565cba7"}, :budget_period {:read true, :write true, :default {:id #uuid "f3b0d720-562c-4158-a8b5-65a4ca84787d", :name "budget_period_I", :inspection_start_date #time/instant "2024-01-21T10:25:08.624239Z", :end_date #time/instant "2024-03-21T10:25:08.624465Z", :created_at #time/instant "2023-12-22T10:25:08.624630Z", :updated_at #time/instant "2023-12-22T10:25:08.624630Z"}, :required true, :value {:id "f3b0d720-562c-4158-a8b5-65a4ca84787d", :name "budget_period_I", :inspection_start_date "2024-01-21T11:25:08.624239+01:00", :end_date "2024-03-21T11:25:08.624465+01:00", :created_at "2023-12-22T11:25:08.62463", :updated_at "2023-12-22T11:25:08.62463", :is_past false}, :request-id #uuid "fce96619-552e-4525-a0bb-b2cf1565cba7"}, :article_name {:read true, :write true, :default nil, :required true, :value "Intelligent Plastic Shoes", :request-id #uuid "fce96619-552e-4525-a0bb-b2cf1565cba7"}, :replacement {:read true, :write true, :default nil, :required true, :value true, :request-id #uuid "fce96619-552e-4525-a0bb-b2cf1565cba7"}, :attachments {:read true, :write true, :required false, :value :unqueried, :request-id #uuid "fce96619-552e-4525-a0bb-b2cf1565cba7"}, :DELETE true, :template {:read true, :write false, :default nil, :required true, :value nil, :request-id #uuid "fce96619-552e-4525-a0bb-b2cf1565cba7"}, :state :NEW, :organization {:value {:id "e5d1d4b3-63a0-4685-b2a7-7518a80e8370", :name "Health, Beauty & Tools", :parent_id "d12fc9b3-80b6-4630-9725-dd6db6af8941", :shortname nil, :department {:id "d12fc9b3-80b6-4630-9725-dd6db6af8941", :name "Sports", :parent_id nil, :shortname nil}}, :read false, :write false, :request-id #uuid "fce96619-552e-4525-a0bb-b2cf1565cba7"}, :supplier_name {:read true, :write true, :default nil, :required false, :value "Schowalter-Homenick", :request-id #uuid "fce96619-552e-4525-a0bb-b2cf1565cba7"}, :order_status {:read true, :write false, :default "NOT_PROCURED", :required true, :value :NOT_PROCESSED, :request-id #uuid "fce96619-552e-4525-a0bb-b2cf1565cba7"}, :requested_quantity {:read true, :write true, :required true, :value 1, :request-id #uuid "fce96619-552e-4525-a0bb-b2cf1565cba7"}, :general_ledger_account {:read false, :write false, :required false, :value nil, :request-id #uuid "fce96619-552e-4525-a0bb-b2cf1565cba7"}, :updated_at {:value #time/instant "2023-12-22T10:25:09.596530Z", :read false, :write false, :request-id #uuid "fce96619-552e-4525-a0bb-b2cf1565cba7"}, :priority {:read true, :write true, :default "NORMAL", :required true, :value :NORMAL, :request-id #uuid "fce96619-552e-4525-a0bb-b2cf1565cba7"}, :id #uuid "fce96619-552e-4525-a0bb-b2cf1565cba7", :total_price_cents 113, :price_currency {:read true, :write false, :default "CHF", :required true, :value "CHF", :request-id #uuid "fce96619-552e-4525-a0bb-b2cf1565cba7"}, :procurement_account {:read false, :write false, :value nil, :request-id #uuid "fce96619-552e-4525-a0bb-b2cf1565cba7"}, :room {:read true, :write true, :default {:id #uuid "b8927fd2-014c-48c2-b095-fa1d5620debe", :name "general room", :description nil, :building_id #uuid "abae04c5-d767-425e-acc2-7ce04df645d1", :general true, :building {:id #uuid "abae04c5-d767-425e-acc2-7ce04df645d1", :name "general building", :code nil}}, :required true, :value {:id "14f4a056-d745-4326-b8b0-bc2b366d4a85", :name "82370", :general false, :building {:id "aaea30d3-c827-49e0-9053-64f1065b965c", :code nil, :name "508 Hoyt Lights"}, :building_id "aaea30d3-c827-49e0-9053-64f1065b965c", :description nil}, :request-id #uuid "fce96619-552e-4525-a0bb-b2cf1565cba7"}, :inspection_comment {:read false, :write false, :required false, :value nil, :request-id #uuid "fce96619-552e-4525-a0bb-b2cf1565cba7"}, :approved_quantity {:read false, :write false, :required false, :value nil, :request-id #uuid "fce96619-552e-4525-a0bb-b2cf1565cba7"}, :order_quantity {:read false, :write false, :required false, :value nil, :request-id #uuid "fce96619-552e-4525-a0bb-b2cf1565cba7"}, :cost_center {:read false, :write false, :required false, :value nil, :request-id #uuid "fce96619-552e-4525-a0bb-b2cf1565cba7"}, :user {:read true, :write false, :required true, :default {:id #uuid "99a8aa98-38b4-44a7-9517-888f96788378", :firstname "Joaquin", :lastname "Volkman"}, :value {:system_admin_protected false, :address nil, :email "haywood@feeney.biz", :pool_protected false, :last_sign_in_at "2023-12-22T11:25:09.913+01:00", :img32_url nil, :account_enabled true, :lastname "Volkman", :phone nil, :img_digest nil, :org_id nil, :extended_info nil, :secondary_email nil, :city nil, :settings nil, :is_admin false, :organization "local", :login nil, :searchable "Joaquin Volkman haywood@feeney.biz    Volkman Joaquin", :updated_at "2023-12-22T11:25:09.913+01:00", :firstname "Joaquin", :zip nil, :id "99a8aa98-38b4-44a7-9517-888f96788378", :url nil, :password_sign_in_enabled true, :account_disabled_at nil, :is_system_admin false, :badge_id nil, :language_locale nil, :img256_url nil, :country nil, :delegator_user_id nil, :created_at "2023-12-22T00:00:00+01:00", :admin_protected false}, :request-id #uuid "fce96619-552e-4525-a0bb-b2cf1565cba7"}, :receiver {:read true, :write true, :required false, :value nil, :request-id #uuid "fce96619-552e-4525-a0bb-b2cf1565cba7"}, :internal_order_number {:read false, :write false, :required false, :value nil, :request-id #uuid "fce96619-552e-4525-a0bb-b2cf1565cba7"}, :created_at {:value #time/instant "2023-12-22T10:25:09.596530Z", :read false, :write false, :request-id #uuid "fce96619-552e-4525-a0bb-b2cf1565cba7"}, :order_comment {:read true, :write false, :required false, :value nil, :request-id #uuid "fce96619-552e-4525-a0bb-b2cf1565cba7"}, :short_id "budget_period_I.005", :model {:read true, :write true, :default nil, :required false, :value nil, :request-id #uuid "fce96619-552e-4525-a0bb-b2cf1565cba7"}} {:category {:read true, :write true, :default {:id #uuid "bdaaea03-8180-4a19-8709-33abef8f26d3", :name "category_1_B", :main_category_id #uuid "35aacc62-c9e9-478a-a396-3a3f00995f7b", :general_ledger_account "5762959998", :cost_center "7429400349", :procurement_account "4912540823"}, :required true, :value {:id "bdaaea03-8180-4a19-8709-33abef8f26d3", :name "category_1_B", :cost_center "7429400349", :main_category {:id "35aacc62-c9e9-478a-a396-3a3f00995f7b", :name "main_category_1"}, :main_category_id "35aacc62-c9e9-478a-a396-3a3f00995f7b", :procurement_account "4912540823", :general_ledger_account "5762959998"}, :request-id #uuid "0f9d5d39-3858-473a-a80f-4f5cc3bc9000"}, :supplier {:read true, :write true, :default nil, :required false, :value nil, :request-id #uuid "0f9d5d39-3858-473a-a80f-4f5cc3bc9000"}, :motivation {:read true, :write true, :required true, :value "Nihil qui accusantium cum.", :request-id #uuid "0f9d5d39-3858-473a-a80f-4f5cc3bc9000"}, :accounting_type {:read false, :write false, :default "aquisition", :required true, :value nil, :request-id #uuid "0f9d5d39-3858-473a-a80f-4f5cc3bc9000"}, :actionPermissions {:edit true, :delete true, :moveBudgetPeriod true, :moveCategory true}, :article_number {:read true, :write true, :default nil, :required false, :value nil, :request-id #uuid "0f9d5d39-3858-473a-a80f-4f5cc3bc9000"}, :inspector_priority {:read false, :write false, :default "MEDIUM", :required true, :value nil, :request-id #uuid "0f9d5d39-3858-473a-a80f-4f5cc3bc9000"}, :price_cents {:read true, :write true, :default 0, :required true, :value 107, :request-id #uuid "0f9d5d39-3858-473a-a80f-4f5cc3bc9000"}, :budget_period {:read true, :write true, :default {:id #uuid "f3b0d720-562c-4158-a8b5-65a4ca84787d", :name "budget_period_I", :inspection_start_date #time/instant "2024-01-21T10:25:08.624239Z", :end_date #time/instant "2024-03-21T10:25:08.624465Z", :created_at #time/instant "2023-12-22T10:25:08.624630Z", :updated_at #time/instant "2023-12-22T10:25:08.624630Z"}, :required true, :value {:id "f3b0d720-562c-4158-a8b5-65a4ca84787d", :name "budget_period_I", :inspection_start_date "2024-01-21T11:25:08.624239+01:00", :end_date "2024-03-21T11:25:08.624465+01:00", :created_at "2023-12-22T11:25:08.62463", :updated_at "2023-12-22T11:25:08.62463", :is_past false}, :request-id #uuid "0f9d5d39-3858-473a-a80f-4f5cc3bc9000"}, :article_name {:read true, :write true, :default nil, :required true, :value "Pandoric Macro", :request-id #uuid "0f9d5d39-3858-473a-a80f-4f5cc3bc9000"}, :replacement {:read true, :write true, :default nil, :required true, :value true, :request-id #uuid "0f9d5d39-3858-473a-a80f-4f5cc3bc9000"}, :attachments {:read true, :write true, :required false, :value :unqueried, :request-id #uuid "0f9d5d39-3858-473a-a80f-4f5cc3bc9000"}, :DELETE true, :template {:read true, :write false, :default nil, :required true, :value nil, :request-id #uuid "0f9d5d39-3858-473a-a80f-4f5cc3bc9000"}, :state :NEW, :organization {:value {:id "da096bdf-563b-4ac5-8615-474fa8394f63", :name "Automotive & Toys", :parent_id "26348e24-5a79-470f-8183-135756e1b158", :shortname nil, :department {:id "26348e24-5a79-470f-8183-135756e1b158", :name "Kids", :parent_id nil, :shortname nil}}, :read false, :write false, :request-id #uuid "0f9d5d39-3858-473a-a80f-4f5cc3bc9000"}, :supplier_name {:read true, :write true, :default nil, :required false, :value "Brown-Torp", :request-id #uuid "0f9d5d39-3858-473a-a80f-4f5cc3bc9000"}, :order_status {:read true, :write false, :default "NOT_PROCURED", :required true, :value :NOT_PROCESSED, :request-id #uuid "0f9d5d39-3858-473a-a80f-4f5cc3bc9000"}, :requested_quantity {:read true, :write true, :required true, :value 2, :request-id #uuid "0f9d5d39-3858-473a-a80f-4f5cc3bc9000"}, :general_ledger_account {:read false, :write false, :required false, :value nil, :request-id #uuid "0f9d5d39-3858-473a-a80f-4f5cc3bc9000"}, :updated_at {:value #time/instant "2023-12-22T10:25:09.581977Z", :read false, :write false, :request-id #uuid "0f9d5d39-3858-473a-a80f-4f5cc3bc9000"}, :priority {:read true, :write true, :default "NORMAL", :required true, :value :NORMAL, :request-id #uuid "0f9d5d39-3858-473a-a80f-4f5cc3bc9000"}, :id #uuid "0f9d5d39-3858-473a-a80f-4f5cc3bc9000", :total_price_cents 214, :price_currency {:read true, :write false, :default "CHF", :required true, :value "CHF", :request-id #uuid "0f9d5d39-3858-473a-a80f-4f5cc3bc9000"}, :procurement_account {:read false, :write false, :value nil, :request-id #uuid "0f9d5d39-3858-473a-a80f-4f5cc3bc9000"}, :room {:read true, :write true, :default {:id #uuid "b8927fd2-014c-48c2-b095-fa1d5620debe", :name "general room", :description nil, :building_id #uuid "abae04c5-d767-425e-acc2-7ce04df645d1", :general true, :building {:id #uuid "abae04c5-d767-425e-acc2-7ce04df645d1", :name "general building", :code nil}}, :required true, :value {:id "fa9ac14b-7b3e-4a3f-81f9-7c11c22578e5", :name "97761", :general false, :building {:id "b9061078-bd62-4a4a-93a9-a10c961699b8", :code nil, :name "824 Welch Mountain"}, :building_id "b9061078-bd62-4a4a-93a9-a10c961699b8", :description nil}, :request-id #uuid "0f9d5d39-3858-473a-a80f-4f5cc3bc9000"}, :inspection_comment {:read false, :write false, :required false, :value nil, :request-id #uuid "0f9d5d39-3858-473a-a80f-4f5cc3bc9000"}, :approved_quantity {:read false, :write false, :required false, :value nil, :request-id #uuid "0f9d5d39-3858-473a-a80f-4f5cc3bc9000"}, :order_quantity {:read false, :write false, :required false, :value nil, :request-id #uuid "0f9d5d39-3858-473a-a80f-4f5cc3bc9000"}, :cost_center {:read false, :write false, :required false, :value nil, :request-id #uuid "0f9d5d39-3858-473a-a80f-4f5cc3bc9000"}, :user {:read true, :write false, :required true, :default {:id #uuid "99a8aa98-38b4-44a7-9517-888f96788378", :firstname "Joaquin", :lastname "Volkman"}, :value {:system_admin_protected false, :address nil, :email "haywood@feeney.biz", :pool_protected false, :last_sign_in_at "2023-12-22T11:25:09.913+01:00", :img32_url nil, :account_enabled true, :lastname "Volkman", :phone nil, :img_digest nil, :org_id nil, :extended_info nil, :secondary_email nil, :city nil, :settings nil, :is_admin false, :organization "local", :login nil, :searchable "Joaquin Volkman haywood@feeney.biz    Volkman Joaquin", :updated_at "2023-12-22T11:25:09.913+01:00", :firstname "Joaquin", :zip nil, :id "99a8aa98-38b4-44a7-9517-888f96788378", :url nil, :password_sign_in_enabled true, :account_disabled_at nil, :is_system_admin false, :badge_id nil, :language_locale nil, :img256_url nil, :country nil, :delegator_user_id nil, :created_at "2023-12-22T00:00:00+01:00", :admin_protected false}, :request-id #uuid "0f9d5d39-3858-473a-a80f-4f5cc3bc9000"}, :receiver {:read true, :write true, :required false, :value nil, :request-id #uuid "0f9d5d39-3858-473a-a80f-4f5cc3bc9000"}, :internal_order_number {:read false, :write false, :required false, :value nil, :request-id #uuid "0f9d5d39-3858-473a-a80f-4f5cc3bc9000"}, :created_at {:value #time/instant "2023-12-22T10:25:09.581977Z", :read false, :write false, :request-id #uuid "0f9d5d39-3858-473a-a80f-4f5cc3bc9000"}, :order_comment {:read true, :write false, :required false, :value nil, :request-id #uuid "0f9d5d39-3858-473a-a80f-4f5cc3bc9000"}, :short_id "budget_period_I.003", :model {:read true, :write true, :default nil, :required false, :value nil, :request-id #uuid "0f9d5d39-3858-473a-a80f-4f5cc3bc9000"}} {:category {:read true, :write true, :default {:id #uuid "24352a74-80ec-4227-af2d-b41a8e8fc71b", :name "category_1_A", :main_category_id #uuid "35aacc62-c9e9-478a-a396-3a3f00995f7b", :general_ledger_account "6895001769", :cost_center "3452424779", :procurement_account "2706997691"}, :required true, :value {:id "24352a74-80ec-4227-af2d-b41a8e8fc71b", :name "category_1_A", :cost_center "3452424779", :main_category {:id "35aacc62-c9e9-478a-a396-3a3f00995f7b", :name "main_category_1"}, :main_category_id "35aacc62-c9e9-478a-a396-3a3f00995f7b", :procurement_account "2706997691", :general_ledger_account "6895001769"}, :request-id #uuid "b88e6c4e-2ccb-4606-b13c-4f8d5d72c791"}, :supplier {:read true, :write true, :default nil, :required false, :value nil, :request-id #uuid "b88e6c4e-2ccb-4606-b13c-4f8d5d72c791"}, :motivation {:read true, :write true, :required true, :value "Dolores dignissimos quia non.", :request-id #uuid "b88e6c4e-2ccb-4606-b13c-4f8d5d72c791"}, :accounting_type {:read false, :write false, :default "aquisition", :required true, :value nil, :request-id #uuid "b88e6c4e-2ccb-4606-b13c-4f8d5d72c791"}, :actionPermissions {:edit true, :delete true, :moveBudgetPeriod true, :moveCategory true}, :article_number {:read true, :write true, :default nil, :required false, :value nil, :request-id #uuid "b88e6c4e-2ccb-4606-b13c-4f8d5d72c791"}, :inspector_priority {:read false, :write false, :default "MEDIUM", :required true, :value nil, :request-id #uuid "b88e6c4e-2ccb-4606-b13c-4f8d5d72c791"}, :price_cents {:read true, :write true, :default 0, :required true, :value 101, :request-id #uuid "b88e6c4e-2ccb-4606-b13c-4f8d5d72c791"}, :budget_period {:read true, :write true, :default {:id #uuid "f3b0d720-562c-4158-a8b5-65a4ca84787d", :name "budget_period_I", :inspection_start_date #time/instant "2024-01-21T10:25:08.624239Z", :end_date #time/instant "2024-03-21T10:25:08.624465Z", :created_at #time/instant "2023-12-22T10:25:08.624630Z", :updated_at #time/instant "2023-12-22T10:25:08.624630Z"}, :required true, :value {:id "f3b0d720-562c-4158-a8b5-65a4ca84787d", :name "budget_period_I", :inspection_start_date "2024-01-21T11:25:08.624239+01:00", :end_date "2024-03-21T11:25:08.624465+01:00", :created_at "2023-12-22T11:25:08.62463", :updated_at "2023-12-22T11:25:08.62463", :is_past false}, :request-id #uuid "b88e6c4e-2ccb-4606-b13c-4f8d5d72c791"}, :article_name {:read true, :write true, :default nil, :required true, :value "Practical Silk Plate", :request-id #uuid "b88e6c4e-2ccb-4606-b13c-4f8d5d72c791"}, :replacement {:read true, :write true, :default nil, :required true, :value true, :request-id #uuid "b88e6c4e-2ccb-4606-b13c-4f8d5d72c791"}, :attachments {:read true, :write true, :required false, :value :unqueried, :request-id #uuid "b88e6c4e-2ccb-4606-b13c-4f8d5d72c791"}, :DELETE true, :template {:read true, :write false, :default nil, :required true, :value nil, :request-id #uuid "b88e6c4e-2ccb-4606-b13c-4f8d5d72c791"}, :state :NEW, :organization {:value {:id "551f6777-c291-45fd-9704-51b593ee90c1", :name "Electronics, Home & Industrial", :parent_id "d8dc0503-5753-4fc5-9702-a7928b9a1e9c", :shortname nil, :department {:id "d8dc0503-5753-4fc5-9702-a7928b9a1e9c", :name "Tools, Books & Games", :parent_id nil, :shortname nil}}, :read false, :write false, :request-id #uuid "b88e6c4e-2ccb-4606-b13c-4f8d5d72c791"}, :supplier_name {:read true, :write true, :default nil, :required false, :value "Pacocha LLC", :request-id #uuid "b88e6c4e-2ccb-4606-b13c-4f8d5d72c791"}, :order_status {:read true, :write false, :default "NOT_PROCURED", :required true, :value :NOT_PROCESSED, :request-id #uuid "b88e6c4e-2ccb-4606-b13c-4f8d5d72c791"}, :requested_quantity {:read true, :write true, :required true, :value 1, :request-id #uuid "b88e6c4e-2ccb-4606-b13c-4f8d5d72c791"}, :general_ledger_account {:read false, :write false, :required false, :value nil, :request-id #uuid "b88e6c4e-2ccb-4606-b13c-4f8d5d72c791"}, :updated_at {:value #time/instant "2023-12-22T10:25:09.550673Z", :read false, :write false, :request-id #uuid "b88e6c4e-2ccb-4606-b13c-4f8d5d72c791"}, :priority {:read true, :write true, :default "NORMAL", :required true, :value :NORMAL, :request-id #uuid "b88e6c4e-2ccb-4606-b13c-4f8d5d72c791"}, :id #uuid "b88e6c4e-2ccb-4606-b13c-4f8d5d72c791", :total_price_cents 101, :price_currency {:read true, :write false, :default "CHF", :required true, :value "CHF", :request-id #uuid "b88e6c4e-2ccb-4606-b13c-4f8d5d72c791"}, :procurement_account {:read false, :write false, :value nil, :request-id #uuid "b88e6c4e-2ccb-4606-b13c-4f8d5d72c791"}, :room {:read true, :write true, :default {:id #uuid "b8927fd2-014c-48c2-b095-fa1d5620debe", :name "general room", :description nil, :building_id #uuid "abae04c5-d767-425e-acc2-7ce04df645d1", :general true, :building {:id #uuid "abae04c5-d767-425e-acc2-7ce04df645d1", :name "general building", :code nil}}, :required true, :value {:id "f0d968ee-3e7c-4d47-82dc-24bb34e0bbae", :name "879", :general false, :building {:id "d6f1dd0e-9d78-4e4b-85de-2496b311f894", :code nil, :name "18771 Quigley Bridge"}, :building_id "d6f1dd0e-9d78-4e4b-85de-2496b311f894", :description nil}, :request-id #uuid "b88e6c4e-2ccb-4606-b13c-4f8d5d72c791"}, :inspection_comment {:read false, :write false, :required false, :value nil, :request-id #uuid "b88e6c4e-2ccb-4606-b13c-4f8d5d72c791"}, :approved_quantity {:read false, :write false, :required false, :value nil, :request-id #uuid "b88e6c4e-2ccb-4606-b13c-4f8d5d72c791"}, :order_quantity {:read false, :write false, :required false, :value nil, :request-id #uuid "b88e6c4e-2ccb-4606-b13c-4f8d5d72c791"}, :cost_center {:read false, :write false, :required false, :value nil, :request-id #uuid "b88e6c4e-2ccb-4606-b13c-4f8d5d72c791"}, :user {:read true, :write false, :required true, :default {:id #uuid "99a8aa98-38b4-44a7-9517-888f96788378", :firstname "Joaquin", :lastname "Volkman"}, :value {:system_admin_protected false, :address nil, :email "haywood@feeney.biz", :pool_protected false, :last_sign_in_at "2023-12-22T11:25:09.913+01:00", :img32_url nil, :account_enabled true, :lastname "Volkman", :phone nil, :img_digest nil, :org_id nil, :extended_info nil, :secondary_email nil, :city nil, :settings nil, :is_admin false, :organization "local", :login nil, :searchable "Joaquin Volkman haywood@feeney.biz    Volkman Joaquin", :updated_at "2023-12-22T11:25:09.913+01:00", :firstname "Joaquin", :zip nil, :id "99a8aa98-38b4-44a7-9517-888f96788378", :url nil, :password_sign_in_enabled true, :account_disabled_at nil, :is_system_admin false, :badge_id nil, :language_locale nil, :img256_url nil, :country nil, :delegator_user_id nil, :created_at "2023-12-22T00:00:00+01:00", :admin_protected false}, :request-id #uuid "b88e6c4e-2ccb-4606-b13c-4f8d5d72c791"}, :receiver {:read true, :write true, :required false, :value nil, :request-id #uuid "b88e6c4e-2ccb-4606-b13c-4f8d5d72c791"}, :internal_order_number {:read false, :write false, :required false, :value nil, :request-id #uuid "b88e6c4e-2ccb-4606-b13c-4f8d5d72c791"}, :created_at {:value #time/instant "2023-12-22T10:25:09.550673Z", :read false, :write false, :request-id #uuid "b88e6c4e-2ccb-4606-b13c-4f8d5d72c791"}, :order_comment {:read true, :write false, :required false, :value nil, :request-id #uuid "b88e6c4e-2ccb-4606-b13c-4f8d5d72c791"}, :short_id "budget_period_I.001", :model {:read true, :write true, :default nil, :required false, :value nil, :request-id #uuid "b88e6c4e-2ccb-4606-b13c-4f8d5d72c791"}}))}])
(def le-bp '[copy-bps "invoked" {:args (({:id #uuid "f3b0d720-562c-4158-a8b5-65a4ca84787d", :name "budget_period_I", :inspection_start_date #time/instant "2024-01-21T10:25:08.624239Z", :end_date #time/instant "2024-03-21T10:25:08.624465Z", :created_at #time/instant "2023-12-22T10:25:08.624630Z", :updated_at #time/instant "2023-12-22T10:25:08.624630Z"}))}])
(def le-dashboard-cache-key '[copy-dashboard-cache-key "invoked" {:args ({:id 1726233604})}])
(def le-copy-main-cats '[copy-main-cats "invoked" {:args (({:id #uuid "35aacc62-c9e9-478a-a396-3a3f00995f7b", :name "main_category_1"} {:id #uuid "98e969ed-e6c1-4205-956e-ed7508a47fe5", :name "main_category_2"}))}])

(comment
  ;; FYI: java.jdbc & old honeySql
  (let [
        tx (db/get-ds-next)

        ;; FIRST STEP
        ;requests (copy-requests requests)
        ;bps (copy-bps bps)
        ;dashboard-cache-key (copy-dashboard-cache-key dashboard-cache-key)
        ;main-cats (copy-main-cats main-cats)


        ;; SECOND STEP
        log-entry-requests (extract-log-entry le-copy-request)
        log-entry-bp (extract-log-entry le-bp)
        log-entry-dashboard-cache-key (extract-log-entry le-dashboard-cache-key)
        log-entry-main-cats (extract-log-entry le-copy-main-cats)

        requests (apply (resolve (first log-entry-requests)) (second log-entry-requests))
        bps (apply (resolve (first log-entry-bp)) (second log-entry-bp))
        dashboard-cache-key (apply (resolve (first log-entry-dashboard-cache-key)) (second log-entry-dashboard-cache-key))
        main-cats (apply (resolve (first log-entry-main-cats)) (second log-entry-main-cats))

        result (create-budget bps tx requests dashboard-cache-key main-cats)

        p (println ">o> result=" result)
        p (println ">o> result::total_price_cents=" (:total_price_cents (first result)))
        p (println ">o> result.count=" (count result))
        ]
    )
  )







