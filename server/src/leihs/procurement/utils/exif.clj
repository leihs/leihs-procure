(ns leihs.procurement.utils.exif
  (:require [clojure.tools.logging :as log]
            [clojure.java.shell :refer [sh]]
            [clojure.string :as string]))

(defn exiftool-version []
  (->> ["exiftool" "-ver"]
       (apply sh)
       :out
       string/trim-newline))

(def exiftool-options ["-j" "-s" "-a" "-u" "-G1"])

(def exiftool-command
  (-> "exiftool"
      (cons exiftool-options)
      vec))

(defn extract-metadata
  [^java.io.File file]
  (->> file
       .getAbsolutePath
       (conj exiftool-command)
       (apply sh)
       :out))
