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
  (branch ""
          (branch "/procure"
                  (leaf "/graphql" :graphql)
                  (leaf "/shutdown" :shutdown)
                  (leaf "/status" :status)
                  (branch "/images/" (param :image-id) (leaf "" :image)))
          ; (leaf "/" :leihs)
          ; (branch "/auth"
          ;         (leaf "" :auth)
          ;         (leaf "/password-sign-in" :auth-password-sign-in)
          ;         (leaf "/sign-out" :auth-sign-out))
          ; (leaf "/manage" :lend)
          ; (leaf "/borrow" :borrow)
          ; (leaf "/" :redirect-to-root)
          (leaf true :not-found)))

;(path-for (paths) :user :user-id "{user-id}")
;(match-route (paths) "/users/512")
;(match-route (paths) "/?x=5#7")

(defn path
  ([kw] (path-for paths kw))
  ([kw route-params]
   (apply (partial path-for paths kw)
     (->> route-params
          (into [])
          flatten)))
  ([kw route-params query-params]
   (str (path kw route-params) "?" (encode-query-params query-params))))
