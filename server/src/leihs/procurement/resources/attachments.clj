(ns leihs.procurement.resources.attachments
  (:require [cheshire.core :refer [generate-string] :rename
             {generate-string to-json}]
            [honey.sql :refer [format] :rename {format sql-format}]
            [honey.sql.helpers :as sql]
            [leihs.core.db :as db]
            [leihs.procurement.paths :refer [path]]
            (leihs.procurement.resources [attachment :as attachment]
              [upload :as upload])
            [leihs.procurement.utils.helpers :refer [cast-to-json]]

            [logbug.debug :as debug]

            [next.jdbc :as jdbc]
            [taoensso.timbre :refer [error info spy warn]]))

(def attachments-base-query
  (-> (sql/select :procurement_attachments.*)
      (sql/from :procurement_attachments)))

(defn get-attachments-for-request-id
  [tx request-id]

  (println ">o> get-attachments-for-request-id" get-attachments-for-request-id)
  (let [query (-> attachments-base-query
                  (sql/where [:= :procurement_attachments.request_id
                              request-id])
                  sql-format
                  spy)
        result (jdbc/execute! tx query)
        p (println ">o> result" result)
        p (println ">o> result" (class result))

        result (->> result
                 (map #(merge % {:url (path :attachment {:attachment-id (:id %)})})))

        ]

    result

    ;(if ((instance? String result))
    ;  result
    ;  result
    ;  )

    ;(->> query
    ;     (jdbc/execute! tx)
    ;     (map #(merge % {:url (path :attachment {:attachment-id (:id %)})}))))
    ))


(defn get-attachments-for-request-id
  [tx request-id]

  (println ">o> get-attachments-for-request-id" get-attachments-for-request-id)
  (let [query (-> attachments-base-query
                  (sql/where [:= :procurement_attachments.request_id
                              request-id])
                  sql-format
                  spy)

        p (println "\n\n>o> result-0" query)
        result (jdbc/execute! tx query)
        p (println ">o> result-1" result)
        p (println ">o> result-2" (class result))]

(->> query
  (jdbc/execute! tx)
  (map #(merge % {:url (path :attachment {:attachment-id (:id %)})})))))



;(ns leihs.my.back.html
;    (:refer-clojure :exclude [keyword str])
;    (:require
;      [hiccup.page :refer [html5]]
;      [honey.sql :refer [format] :rename {format sql-format}]
;      [honey.sql.helpers :as sql]
;      [leihs.core.http-cache-buster2 :as cache-buster]
;      [leihs.core.json :refer [to-json]]
;      [leihs.core.remote-navbar.shared :refer [navbar-props]]
;      [leihs.core.shared :refer [head]]
;      [leihs.core.url.core :as url]
;      [leihs.my.authorization :as auth]
;      [leihs.core.db :as db]
;      [next.jdbc :as jdbc]))

(comment

  (let [
        tx (db/get-ds-next)
        req_id #uuid "95528687-f538-5618-b3eb-98bba5c904c7"
        req_id #uuid "5f2da6e2-9125-40ea-92e8-88bf4532fb6d" ; ERROR

        res (get-attachments-for-request-id tx req_id)


        ;request {:route-params {:user-id #uuid "c0777d74-668b-5e01-abb5-f8277baa0ea8"}
        ;         :tx           tx}
        ;user-id #uuid "37bb3d3d-3a61-4f98-863e-c549568317f0"
        ;query (sql-format {:select :*
        ;                   :from [:users]
        ;                   :where [:= :id [:cast user-id :uuid]]})
        ;
        ;query2 (-> (sql/select :*)
        ;           (sql/from :users)
        ;           (sql/where [:= :id user-id])
        ;           sql-format
        ;           (->> (jdbc/execute! tx))
        ;           )
        ;
        ;p (println "\nquery" query)
        p (println "\n res" res)
        ]
    res
    )
  )


(defn get-attachments
  [context _ value]

  (println ">o> get-attachments value=" value)
  (let [tx (-> context
               :request
               :tx-next)]
    (get-attachments-for-request-id tx (:request-id value))))

(defn create-for-request-id-and-uploads!
  [tx req-id uploads]
  (println ">o> create-for-request-id-and-uploads! uploads=" uploads)
  (doseq [{u-id :id} uploads]
    (let [u-row (upload/get-by-id tx u-id)
          md (-> u-row
                 :metadata
                 to-json
                 cast-to-json)]
      (attachment/create! tx
        (-> u-row
            (dissoc :id)
            (dissoc :created_at)
            (assoc :metadata md)
            (assoc :request_id req-id)))
      (upload/delete! tx u-id))))

(defn delete!
  [tx ids]
  (println ">o> delete!")
  (jdbc/execute! tx
    (-> (sql/delete-from :procurement_attachments)
        (sql/where [:in :procurement_attachments.id ids])
        sql-format)))

(debug/debug-ns *ns*)