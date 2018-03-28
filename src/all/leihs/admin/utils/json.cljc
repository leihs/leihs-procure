(ns leihs.admin.utils.json
  (:require
    #?(:clj [cheshire.core])
    #?(:clj [leihs.admin.utils.json-protocol])
    [clojure.walk]))

(defn to-json [d]
  #?(:clj (cheshire.core/generate-string d)
     :cljs (js/JSON.stringify d)))

(defn from-json [s]
  (clojure.walk/keywordize-keys
    #?(:clj (cheshire.core/parse-string s)
       :cljs (js/JSON.parse s))))

