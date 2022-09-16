(ns leihs.admin.scratch
  (:refer-clojure :exclude [str keyword])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [clojure.string :as string]))

(def pools [{:id "8a4fa6e3-2b36-46b9-9508-212d4f5125c6", :name "AV-Services", :index 1}
            {:id "82a48074-8a4d-5d3b-b1dd-8e35954137e8", :name "AVS-Ausstattung", :index 2}
            {:id "c092bbb0-3a12-4412-869f-ea1686bd059f", :name "AVS-Dauerausleihen", :index 3}
            {:id "b451c4b8-bd20-40cd-bb08-42b3ccee7bd6", :name "AVS-IASpace", :index 4}
            {:id "980e5637-37e8-5adc-a692-fd0cc2cc7f25", :name "AVS-Inventar", :index 5}
            {:id "b534b07c-d9bf-4572-840c-0a630b8cd8ed", :name "AVS-Museum/Ausstellungen", :index 6}
            {:id "27b7e10b-66ad-5dcc-ae73-4b11551dadfe", :name "AVS-Veranstaltungstechnik", :index 7}
            {:id "8bd16d45-056d-5590-bc7f-12849f034351", :name "Ausleihe Toni-Areal", :index 8}
            {:id "76d54c17-49f1-55e8-b6af-2e9dc0209be3", :name "DDK - Filmstudio 1.J33", :index 9}
            {:id "c7dedd05-a08c-5910-8e67-5cc9c250acfd", :name "DDK - Veranstaltungstechnik", :index 10}
            {:id "57f51769-0e08-5abe-aaba-61a872477f36", :name "DDK Theater Technik AV", :index 11}
            {:id "667a93e6-496a-59c0-8315-d51d44973e3c", :name "DMU - ICST", :index 12}
            {:id "eb64a67b-ff75-5d57-b043-074645d7b45e", :name "Departement Kulturanalysen und -vermittlung", :index 13}
            {:id "ebca1932-f85a-54d4-a61b-2dbbb3e8cb54", :name "Departement Musik", :index 14}
            {:id "6c046295-6d69-4ac7-87f6-3a51126193c7", :name "E-Learning", :index 15}
            {:id "e3dadec7-12a2-52f6-8d6f-475ea5d74ee7", :name "Farb-Licht Zentrum", :index 16}
            {:id "a02b8163-9a16-5066-b48e-9eb74cf8b791", :name "Fundus-DDK", :index 17}
            {:id "ed324c19-b5e8-539d-8dc6-87fb09022a5a", :name "ICST-Ausleihe", :index 18}
            {:id "5dd25b58-fa56-5095-bd97-2696d92c2fb1", :name "IT-Zentrum", :index 19}
            {:id "4a1ba40c-467e-5efe-8cf1-e8d3dbb59f04", :name "ITZ-Ausstellungen", :index 20}
            {:id "3977012c-ce0e-501f-889b-8715fdb5d83b", :name "ITZ-Software", :index 21}
            {:id "1ae0a2bf-cd0f-51de-a121-8991d24ad90a", :name "Maschinen", :index 22}
            {:id "88c43fa9-a9ba-5812-b9eb-a6b649905884", :name "Modellbau", :index 23}
            {:id "c1ef75b1-a024-5bcb-ad04-dd76affabad2", :name "Musikklub Mehrspur", :index 24}
            {:id "f67266a6-d4ce-5bbf-8cc0-1c2ae008cc20", :name "Vertiefung Mediale Künste", :index 25}
            {:id "03616625-b31f-5229-bb1a-9b4f28eb1566", :name "Vertiefung Szenografie", :index 26}
            {:id "77e88ec8-9ff6-5435-818e-dc902fc631a6", :name "Werkstatt Ausleihe", :index 27}
            {:id "91affef6-57fd-5cd9-8646-b29ac905f4b7", :name "ZHdK-Inventar", :index 28}])

(comment 
  (let [tmp (->> pools
                 (map #(do [(:id %) (:name %)]))
                 (into {}))]
    (sorted-map-by (fn [key1 key2] (compare (get tmp key1) (get tmp key2))) tmp))

  (->> pools (map :name) sort))
