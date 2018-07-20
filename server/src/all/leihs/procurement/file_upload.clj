(ns leihs.procurement.file-upload
  (:require [clojure.tools.logging :as log]
            [compojure.core :as cpj]
            [leihs.procurement.paths :refer [path]])
  (:import [java.util Base64]
           [org.apache.commons.io FileUtils]))

(defn file-upload
  [{params :params}]
  (->> params
       :upload
       :tempfile
       (FileUtils/readFileToByteArray)
       (.encodeToString (Base64/getMimeEncoder))
       log/debug)
  {:body "OK"})

(def routes (cpj/routes (cpj/POST (path :file-upload) [] #'file-upload)))
