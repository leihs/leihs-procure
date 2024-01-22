(ns leihs.procurement.paths
  (:refer-clojure :exclude [str keyword])
  (:require [bidi [bidi :refer [path-for]] [verbose :refer [branch leaf param]]]
            leihs.core.paths
            [leihs.procurement.utils.core :refer [str]]
            [leihs.procurement.utils.url.query-params :refer
             [encode-query-params]]))

(def paths
  (branch
   ""
   leihs.core.paths/core-paths
   (branch
    "/procure"
    (leaf "/shutdown" :shutdown)
    (leaf "/upload" :upload)
    (leaf "/graphql" :graphql)
    (leaf "/status" :status)
    (branch "/attachments/" (param :attachment-id) (leaf "" :attachment))
      ; NOTE: don't rename the handler-key for image as it may break the
      ; workaround for the problem with hanging requests
    (branch "/images/" (param :image-id) (leaf "" :image)))
   (leaf true :not-found)))

(reset! leihs.core.paths/paths* paths)

(def path leihs.core.paths/path)
