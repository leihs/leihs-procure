(ns leihs.admin.utils.url.query-params
  (:refer-clojure :exclude [str keyword encode decode])
  (:require
    [leihs.admin.utils.core :refer [keyword str presence]]
    [leihs.admin.utils.json :refer [to-json from-json try-parse-json]]

    [clojure.walk :refer [keywordize-keys]]
    [clojure.string :as string]
    [leihs.admin.utils.url.shared :as shared])) 

(defn decode-query-params [query-string & {:keys [parse-json?]
                                            :or {parse-json? true}}]
  (let [parser (if parse-json? try-parse-json identity)]
    (->> (if-not (presence query-string) [] (string/split query-string #"&"))
         (reduce
           (fn [m part]
             (let [[k v] (string/split part #"=" 2)]
               (assoc m (-> k shared/decode keyword) 
                      (-> v shared/decode parser))))
           {})
         keywordize-keys)))

(defn encode-query-params [params]
  (->> params
       (map (fn [[k v]]
              (str (-> k str shared/encode)
                   "="
                   (-> (if (coll? v) (to-json v) v)
                       str shared/encode))))
       (clojure.string/join "&")))
