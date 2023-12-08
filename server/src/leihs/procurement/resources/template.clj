(ns leihs.procurement.resources.template
  (:require

    ;[clojure.java.jdbc :as jdbc]
            [leihs.procurement.utils.sql :as sqlp]

                [taoensso.timbre :refer [debug info warn error spy]]


          [honey.sql :refer [format] :rename {format sql-format}]
          [leihs.core.db :as db]
          [next.jdbc :as jdbc]
          [honey.sql.helpers :as sql]

            [clojure.data :refer [diff]]
            [leihs.core.core :refer [raise]]
            [taoensso.timbre :refer [debug info warn error]]))

(def ALLOWED-KEYS-FOR-USED-TEMPLATE #{:is_archived})

(def templates-base-query
  (-> (sql/select :procurement_templates.*)
      (sql/from :procurement_templates)))

(defn get-template-by-id
  [tx id]

  (println ">o> tocheck ??? get-template-by-id" id)



  (spy (-> templates-base-query
      (sql/where [:= :procurement_templates.id [:cast id :uuid]])
      sql-format
      (->> (jdbc/execute! tx))
      )))

(defn my-cast [data]
  (println ">o> no / my-cast /debug " data)
  (if (contains? data :category_id)
    (let [
          p (println ">o> no before _> room_id=" (:category_id data))
          ;(assoc data :room_id (java.util.UUID/fromString (:room_id data)))
          ;(assoc data :room_id [:cast (:room_id data) :uuid])
          data (assoc data :category_id [[:cast (:category_id data) :uuid]])
          p (println ">o> no after _> room_id=" data)
          ] data)
    data
    )
  )

(defn insert-template!
  [tx tmpl]
  (spy (-> (jdbc/execute! tx
                 (-> (sql/insert-into :procurement_templates)
                     (sql/values [(my-cast tmpl)])
                     sql-format)

                 )
      :update-count
      ))

  )

(defn validate-update-attributes [tx tmpl]
  (let [
        res (-> (sql/select :%count.*)
                       (sql/from :procurement_requests)
                       (sql/where [:= :template_id [:cast(:id tmpl):uuid]])
                       sql-format
                       (->> (jdbc/execute-one! tx))
                       )
        p (println ">o> res - :count needed!!!!!!!!" res)

        req-exist? (-> (sql/select :%count.*)
                       (sql/from :procurement_requests)
                       (sql/where [:= :template_id [:cast(:id tmpl):uuid]])
                       sql-format
                       (->> (jdbc/execute-one! tx))
                       :count (> 0) )



        ]
    (if req-exist?
      (select-keys tmpl ALLOWED-KEYS-FOR-USED-TEMPLATE))
      tmpl))

(defn update-template!
  [tx tmpl]
  (spy (jdbc/execute! tx
                 (-> (sql/update :procurement_templates)
                     (sql/set (validate-update-attributes tx tmpl))
                     (sql/where [:= :procurement_templates.id [:cast (:id tmpl) :uuid]])
                     sql-format))))

(defn delete-template!
  [tx id]
   (spy (jdbc/execute! tx
                 (-> (sql/delete-from :procurement_templates)
                     (sql/where [:= :procurement_templates.id [:cast id :uuid]])
                     sql-format))))

(defn get-template
  ([context _ value]

   (println ">o> 0 tocheck ??? get-template" value)

   (get-template-by-id (-> context
                           :request
                           :tx-next)
                       (or (:value value) ; for RequestFieldTemplate
                           (:template_id value))))
  ([tx tmpl]
   (println ">o> 1 tocheck ??? get-template")


   (let [where-clause (sqlp/map->where-clause :procurement_templates tmpl)]
     (spy (-> templates-base-query
         (sql/where where-clause)
         sql-format
         spy
         (->> (jdbc/execute! tx))
         ))

     )))

(defn requests-count [{{:keys [tx]} :request} _ {:keys [id]}]

  (println ">o> requests-count tocheck count!!!!! _> id=" id)



(spy  (-> (sql/select :%count.*)
      (sql/from :procurement_requests)
      (sql/where [:= :template_id [:cast id :uuid]])
      sql-format
      (->> (jdbc/execute-one! tx))
      :count))

  )
