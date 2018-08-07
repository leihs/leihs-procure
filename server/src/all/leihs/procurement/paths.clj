(ns leihs.procurement.paths
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.procurement.utils.core :refer [keyword str presence]]
            [bidi.verbose :refer [branch param leaf]]
            [bidi.bidi :refer [path-for match-route]]
            [leihs.procurement.utils.url.query-params :refer
             [encode-query-params]]
            [uritemplate-clj.core :as uri-templ]
            [clojure.tools.logging :as logging]
            [logbug.catcher :as catcher]
            [logbug.debug :as debug]
            [logbug.thrown :as thrown]))

(def paths
  (branch
    ""
    (branch
      "/procure"
      (leaf "/upload" :upload)
      (leaf "/graphql" :graphql)
      (leaf "/status" :status)
      (branch "/attachments/" (param :attachment-id) (leaf "" :attachment))
      ; NOTE: don't rename the handler-key for image as it may break the
      ; workaround for the problem with hanging requests
      (branch "/images/" (param :image-id) (leaf "" :image)))
    (leaf true :not-found)))

(defn path
  ([kw] (path-for paths kw))
  ([kw route-params]
   (apply (partial path-for paths kw)
     (->> route-params
          (into [])
          flatten)))
  ([kw route-params query-params]
   (str (path kw route-params) "?" (encode-query-params query-params))))
