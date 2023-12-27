(ns leihs.procurement.utils.helpers
  (:require [clojure.set :refer [subset?]]

            [taoensso.timbre :refer [debug error info spy warn]]

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
        ;uuids-sql (map (fn [uuid-str] [:cast uuid-str :uuid]) (set uuids))

        ;; TODO: test it, avoid to make unique
        ;uuids (set uuids)

        uuids-sql (map (fn [uuid-str] [:cast uuid-str :uuid]) uuids)
        p (println ">o> uuids-sql" uuids-sql)
        ]
    (spy uuids-sql)
    )
  ;(spy (map (fn [uuid-str] [:cast uuid-str :uuid]) (set uuids)))
  )

(comment
  (let [
        id1 #uuid "3eaba478-f710-4cb8-bc87-54921a27e3b2"    ;; >>3 []
        id2 #uuid "3eaba478-f710-4cb8-bc87-54921a27e3bb"    ;; >>3 []


        result (cast-uuids [id1 id2 id1])

        p (println ">o> result=" result)
        p (println ">o> result.count=" (count result))
        ] result)
  )

(import [java.time ZonedDateTime]
        [java.time.format DateTimeFormatter]
        [java.sql Timestamp]
        [java.time Instant]
        [java.time ZoneId])

(defn timestamp-to-zoneddatetime [timestamp]
  (let [instant (.toInstant timestamp)
        zone-id (ZoneId/of "Z")]                            ; UTC
    (ZonedDateTime/ofInstant instant zone-id)))

(defn format-date [timestamp]
  (println ">>> xxx" timestamp)
  (println ">>> xxx class=" (class timestamp))

  (.format (timestamp-to-zoneddatetime timestamp) (DateTimeFormatter/ISO_INSTANT)))


; [leihs.procurement.utils.helpers :refer [convert-dates]]
(defn convert-dates [entry]
  (-> entry
      (update :start_date #(if (contains? entry :start_date) (format-date %)))
      (update :end_date #(if (contains? entry :end_date) (format-date %)))
      (update :created_at #(if (contains? entry :created_at) (format-date %)))
      (update :inspection_start_date #(if (contains? entry :inspection_start_date) (format-date %)))
      (update :updated_at #(if (contains? entry :updated_at) (format-date %)))))
