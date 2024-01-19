(ns leihs.procurement.utils.url.query-params
  (:refer-clojure :exclude [str keyword encode decode])
  (:require cheshire.core
            [clojure [string :as string] [walk :refer [keywordize-keys]]]
            [leihs.procurement.utils.core :refer [keyword presence str]]
            [leihs.procurement.utils.url.shared :as shared]))

(defn try-parse-json
  [value]
  (try (cheshire.core/parse-string value) (catch Exception _ value)))

(defn to-json [x] (cheshire.core/generate-string x))

(defn decode-query-params
  [query-string]
  (->> (if-not (presence query-string) [] (string/split query-string #"&"))
       (reduce
         (fn [m part]
           (let [[k v] (string/split part #"=" 2)]
             (assoc m
               (-> k
                   shared/decode
                   keyword)
                 (-> v
                     shared/decode
                     try-parse-json))))
         {})
       keywordize-keys))

(defn encode-query-params
  [params]
  (->> params
       (map (fn [[k v]]
              (str (-> k
                       str
                       shared/encode)
                   "="
                   (-> (if (coll? v) (to-json v) v)
                       str
                       shared/encode))))
       (clojure.string/join "&")))
