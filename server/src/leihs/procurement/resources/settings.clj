(ns leihs.procurement.resources.settings
  (:require
    ;[clojure.java.jdbc :as jdbc]
    [honey.sql :refer [format] :rename {format sql-format}]

    [honey.sql.helpers :as sql]


    [leihs.core.db :as db]
    [leihs.procurement.utils.sql :as sqlo]
    [next.jdbc :as jdbc]
    [taoensso.timbre :refer [debug error info spy warn]]

    ))

(def settings-base-query
  (-> (sql/select :procurement_settings.*)
      (sql/from :procurement_settings)))

(defn get-settings
  ([context _ _]
   (get-settings (-> context
                     :request
                     :tx-next)))
  ([tx]
   (-> settings-base-query
       sql-format
       (->> (jdbc/execute-one! tx))
       )))

(defn update-settings!
  [context args value]
  (let [tx (-> context
               :request
               :tx-next)
        input-data (:input_data args)
        inspection-comments (->> input-data
                                 :inspection_comments
                                 (map (fn [comment] [:cast comment :text]))
                                 (cons :jsonb_build_array))
        settings (-> input-data
                     (assoc :inspection_comments inspection-comments))]
    (jdbc/execute-one! tx (-> (sql/update :procurement_settings)
                              (sqlo/sset (spy settings))
                              sql-format
                              spy
                              ))
    (get-settings tx)

    ))








(comment

  (let [
        tx (db/get-ds-next)
        request {:route-params {:user-id #uuid "c0777d74-668b-5e01-abb5-f8277baa0ea8"}
                 :tx-next tx}

        ;; master (old version)
        ;inspection-comments (->> input-data
        ;                         :inspection_comments
        ;                         (map #(sql/call :cast % :text))
        ;                         (cons :jsonb_build_array)
        ;                         (apply sql/call))


        input-data {:contact_url "https://intern.zhdk.ch/index.php?id=leihs_bedarfsermittlung", :inspection_comments ["Projektidee." "Projektauftrag muss erarbeitet werden (M20).abc222333"]}

        inspection-comments (->> input-data
                                 :inspection_comments
                                 (map (fn [uuid-str] [:cast uuid-str :text]))
                                 (cons :jsonb_build_array)
                                 )

        p (println "\n>o> inspection-comments1" inspection-comments)

        settings (-> (spy input-data)
                     (assoc :inspection_comments inspection-comments))

        result (jdbc/execute-one! tx (-> (sql/update :procurement_settings)
                                         (sqlo/sset settings)
                                         sql-format
                                         spy
                                         ))

        p (println "\nquery" (spy result))


        p (spy (:next.jdbc/update-count result))            ;;1
        p (spy (:update-count result))                      ;;nil

        ]
    )
  )



(comment

  (let [
        tx (db/get-ds-next)
        request {:route-params {:user-id #uuid "c0777d74-668b-5e01-abb5-f8277baa0ea8"}
                 :tx-next tx}

        ;; master (old version)
        ;inspection-comments (->> input-data
        ;                         :inspection_comments
        ;                         (map #(sql/call :cast % :text))
        ;                         (cons :jsonb_build_array)
        ;                         (apply sql/call))


        input-data {:contact_url "https://intern.zhdk.ch/index.php?id=leihs_bedarfsermittlung", :inspection_comments ["Projektidee." "Projektauftrag muss erarbeitet werden (M20).abc222"]}

        inspection-comments (->> input-data
                                 :inspection_comments
                                 ;(map #([:cast % :text]))
                                 ;(cons :jsonb_build_array)
                                 )

        p (println "\n>o> inspection-comments1" inspection-comments)



        ;inspection-comments #([:cast % :text] inspection-comments)

        inspection-comments (map (fn [uuid-str] [:cast uuid-str :text]) inspection-comments)
        p (println "\n>o> inspection-comments2a" inspection-comments)

        inspection-comments (cons :jsonb_build_array inspection-comments)


        ;inspection-comments (map inspection-comments)
        ;p (println "\n>o> inspection-comments2b" inspection-comments)


        ;inspection-comments [[inspection-comments ]]
        ;inspection-comments [:cast inspection-comments :jsonb]
        inspection-comments [inspection-comments]

        p (println "\n>o> inspection-comments3" inspection-comments)


        ;settings inspection-comments
        p (println "\n>o> query" (sql-format inspection-comments))


        settings (-> (spy input-data)
                     (assoc :inspection_comments inspection-comments))
        ;;p (println "\n>o> settings" settings)
        ;;
        ;;
        result (jdbc/execute! tx
                              (-> (sql/update :procurement_settings)
                                  (sqlo/sset settings)
                                  sql-format
                                  spy
                                  ))

        ;>o> settings {:contact_url https://intern.zhdk.ch/index.php?id=leihs_bedarfsermittlung, :inspection_comments #sql/call [:jsonb_build_array #sql/call [:cast Projektidee. Projektauftrag muss erarbeitet werden (M20).abc222 :text]]}
        ;[leihs.procurement.resources.settings:?] - (sql/format (sql/sset (sql/update :procurement_settings) settings)) => ["UPDATE procurement_settings SET contact_url = ?, inspection_comments = jsonb_build_array(CAST(? AS text)) " "https://intern.zhdk.ch/index.php?id=leihs_bedarfsermittlung" "Projektidee. Projektauftrag muss erarbeitet werden (M20).abc222"]
        ;result => (1)

        ;result => (1)
        p (println "\nquery" (spy result))
        ]
    )
  )

