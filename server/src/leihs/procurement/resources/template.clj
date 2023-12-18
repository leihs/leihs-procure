(ns leihs.procurement.resources.template
  (:require

    ;[clojure.java.jdbc :as jdbc]
    [leihs.procurement.utils.sql :as sqlp]

    [leihs.procurement.utils.helpers :refer [my-cast]]

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

  (println ">oo> tocheck ??? get-template-by-id" id)

  (spy (-> templates-base-query
           (sql/where [:= :procurement_templates.id [:cast id :uuid]])
           sql-format
           (->> (jdbc/execute-one! tx))
           )))


(defn insert-template!
  [tx tmpl]

  (println ">oo> ??????????4 tmpl=" tmpl)


  (let [tmpl (my-cast tmpl)

        p (println ">oo> ??????????5 tmpl=" tmpl)


        result (spy (-> (jdbc/execute! tx (-> (sql/insert-into :procurement_templates)
                                              (sql/values [tmpl])
                                              sql-format
                                              ;spy
                                              )
                                       )
                        ))

        result (:update-count result)

        ]
    (spy result)
    )

  )

(defn validate-update-attributes [tx tmpl]
  (spy (let [
        p (println ">oo> ??????????1 tmpl=" tmpl)
        p (println ">oo> ??????????2 tmpl=" (:id tmpl))
        result (spy (-> (sql/select :%count.*)
                   (sql/from :procurement_requests)
                   ;(sql/where [:= :template_id [:cast (:id tmpl) :uuid]])
                   (sql/where [:= :template_id (:id tmpl)])
                   sql-format
                   ;spy
                   (->> (jdbc/execute-one! tx))
                   ))


        req-exist? (spy (-> (spy result)
                            :count
                            (> 0)
                            ))

        p (println ">o> res - true/false needed!!!!!!!!" (spy req-exist?))

        ]
    (if req-exist?
      (select-keys tmpl ALLOWED-KEYS-FOR-USED-TEMPLATE))

    tmpl)))





(defn update-template!
  [tx tmpl]

  (let [
        p (println ">oo> update-template! ???????? tmpl=" tmpl)

        casted-tmpl (my-cast (spy tmpl))
        casted-tmpl (validate-update-attributes tx casted-tmpl)

        ]

    (spy tmpl)

    (spy (-> (spy (jdbc/execute-one! tx (-> (sql/update :procurement_templates)
                                            (sql/set (spy casted-tmpl))
                                            (sql/where [:= :procurement_templates.id (:id casted-tmpl)])
                                            sql-format
                                            ;spy
                                            )))
             :next.jdbc/update-count
             ;:update-count
             list
             ))
    )

  )

(defn delete-template!
  [tx id]
  (spy (jdbc/execute! tx (-> (sql/delete-from :procurement_templates)
                             (sql/where [:= :procurement_templates.id [:cast id :uuid]])
                             sql-format
                             spy))))

(defn get-template
  ([context _ value]

   (println ">oo> 0 tocheck ??? get-template" value)

   (spy (get-template-by-id (-> context
                           :request
                           :tx-next)
                       (or (:value value)                   ; for RequestFieldTemplate
                           (:template_id value)))))

  ([tx tmpl]
   (println ">oo> 1 tocheck ??? get-template")


   (let [
         tmpl (my-cast tmpl)
         where-clause (sqlp/map->where-clause :procurement_templates (spy tmpl))]
     (spy (-> templates-base-query
              (sql/where where-clause)
              sql-format
              spy
              (->> (jdbc/execute! tx))
              ))

     )))

(defn requests-count [{{:keys [tx]} :request} _ {:keys [id]}]

  (println ">oo> requests-count tocheck count!!!!! _> id=" id)


  (spy (-> (sql/select :%count.*)
           (sql/from :procurement_requests)
           (sql/where [:= :template_id [:cast id :uuid]])
           sql-format
           (->> (jdbc/execute-one! tx))
           :count))

  )
