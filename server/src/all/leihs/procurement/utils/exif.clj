(ns leihs.procurement.utils.exif
  (:require [clojure.java.shell :refer [sh]]
            [clojure.string :as string]))

(defn- replace-group-string
  [s]
  "[foo] bar => foo:bar"
  (string/replace s #"\[(.*)]\s*(.*)" "$1:$2"))

(defn extract-metadata
  [^java.io.File file]
  (->> file
       .getAbsolutePath
       (sh "exiftool" "-s" "-a" "-u" "-G1")
       :out
       string/split-lines
       (map replace-group-string)
       (map #(string/split % #":\s"))
       ; -------------------------------------
       ; sometimes there is nothing behind `:`
       (map #(if (= (count %) 1) (conj % "") %))
       ; -------------------------------------
       flatten
       (map string/trim)
       (apply hash-map)))
