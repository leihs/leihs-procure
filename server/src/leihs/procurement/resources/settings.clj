(ns leihs.procurement.resources.settings
  (:require [clojure.java.jdbc :as jdbc]

                [taoensso.timbre :refer [debug info warn error spy]]

            [leihs.core.db :as db]


            [leihs.procurement.utils.sql :as sql]))

(def settings-base-query
  (-> (sql/select :procurement_settings.*)
      (sql/from :procurement_settings)))

(defn get-settings
  ([context _ _]
   (get-settings (-> context
                     :request
                     :tx)))
  ([tx]
   (-> settings-base-query
       sql/format
       (->> (jdbc/query tx))
       first)))

(defn update-settings!
  [context args value]
  (let [tx (-> context
               :request
               :tx)


        input-data (:input_data args)
        p (println ">o> input-data" input-data)



        ;>o> inspection-comments #sql/call [:jsonb_build_array #sql/call [:cast Projektidee. Projektauftrag muss erarbeitet werden (M20).abc :text]
        ;                                   #sql/call [:cast Bereits bewilligtes Projekt (HSL) :text] #sql/call [:cast Sicherheitsrelevant / gesetzliche Vorgabe :text]
        ;                                   #sql/call [:cast Projekt PPM (A-oder B-Projekt) :text] #sql/call [:cast Vorbezug laufendes Budget :text]
        ;                                   #sql/call [:cast In Sammelposition "Bauliche Anpassungen und Mobiliar" enthalten :text]
        ;                                   #sql/call [:cast In Sammelposition "IT & Elektronik" enthalten :text] #sql/call [:cast In Sammelposition "Beleuchtungstechnik" enthalten :text] #sql/call [:cast In Sammelposition "Audio- und Videotechnik" enthalten :text] #sql/call [:cast In Sammelposition "Musikinstrumente" enthalten :text] #sql/call [:cast In Sammelposition "Fotografie" enthalten :text] #sql/call [:cast In Sammelposition "Beleuchtungstechnik" enthalten :text] #sql/call [:cast In Sammelposition "Werkstatt-Technik" enthalten :text] #sql/call [:cast In Sammelposition "B��hnentechnik" enthalten :text] #sql/call [:cast Im Rahmen der finanziellen Vorgaben kann dieser Antrag nicht bewilligt werden :text] #sql/call [:cast Im Rahmen der finanziellen Vorgaben kann dieser Antrag nicht bewilligt werden, Bedarf kann aus Ausleihe gedeckt werden :text] #sql/call [:cast Im Rahmen der finanziellen Vorgaben kann dieser Antrag nicht bewilligt werden, alternative L��sungen vorhanden :text] #sql/call [:cast Verbrauchsmaterial, auf eigene Kostenstelle budgetieren :text] #sql/call [:cast Projekt wird nicht durchgef��hrt :text] #sql/call [:cast Ger��t kann repariert werden :text] #sql/call [:cast Service��nderungsantrag notwendig :text] #sql/call [:cast Betriebskonzept liegt nicht vor :text] #sql/call
        ;  [:cast Digitalisierungsstrategie inkl. Umsetzungsmassnahmen und Kostenrahmen von HSL genehmigen lassen  :text]]

        inspection-comments (->> input-data
                                 :inspection_comments
                                 (map #(sql/call :cast % :text))
                                 (cons :jsonb_build_array)
                                 (apply sql/call))
        p (println ">o> inspection-comments" inspection-comments)

        settings (-> (spy input-data)
                     (assoc :inspection_comments inspection-comments))
        p (println ">o> settings" settings)]


    (jdbc/execute! tx
                   (-> (sql/update :procurement_settings)
                       (sql/sset settings)
                       sql/format))
    (get-settings tx)))


(comment

  (let [
        tx (db/get-ds)
        request {:route-params {:user-id #uuid "c0777d74-668b-5e01-abb5-f8277baa0ea8"}
                 :tx           tx}


        input-data {:contact_url "https://intern.zhdk.ch/index.php?id=leihs_bedarfsermittlung", :inspection_comments ["Projektidee. Projektauftrag muss erarbeitet werden (M20).abc222"]}

        inspection-comments (->> input-data
                                 :inspection_comments
                                 (map #(sql/call :cast % :text))
                                 (cons :jsonb_build_array)
                                 (apply sql/call))

        p (println "\n>o> inspection-comments" inspection-comments)

        settings (-> (spy input-data)
                     (assoc :inspection_comments inspection-comments))
        p (println "\n>o> settings" settings)


        result (jdbc/execute! tx
                   (-> (sql/update :procurement_settings)
                       (sql/sset settings)
                       sql/format
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

