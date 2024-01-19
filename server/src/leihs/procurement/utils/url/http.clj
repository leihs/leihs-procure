(ns leihs.procurement.utils.url.http
  (:require
    [clojure.set]
    [leihs.procurement.utils.url.shared :refer [auth-dissect host-port-dissect parse-int path-dissect]]
    [clj-yaml.core :as yaml]))

(def pattern
  #"(?i)(https?)://([^@]+@)?([^/^\?]+)(/[^\?^#]*)?(\?[^#]+)?(#.*)?"
  ; 1.protocol     2.auth 3.host-port 4.path  5.query 6.fragment
  )

; TODO drop silly props; add :query-params with map;
;        ... once all the tests are passing

(defn dissect-basic-http-url
  [url]
  (as-> url url
    (re-matches pattern url)
    {:protocol (-> url
                   (nth 1)
                   clojure.string/lower-case),
     :authentication_with_at (nth url 2),
     :host_port (nth url 3),
     :path (nth url 4),
     :query (nth url 5),
     :fragment_with_hash (nth url 6),
     :parts url,
     :url (nth url 0)}))

(defn dissect
  [url]
  (as-> url url
    (dissect-basic-http-url url)
    (merge url (host-port-dissect (:host_port url)))
    (merge url (auth-dissect (:authentication_with_at url)))
    (merge url (path-dissect (:path url)))))

(defn parse-query
  [query-string]
  (->> (clojure.string/replace query-string #"^\?" "")
       ring.util.codec/form-decode
       clojure.walk/keywordize-keys
       (map (fn [[qk qv]] [qk (yaml/parse-string qv)]))
       (into {})))

(defn parse-base-url
  [url]
  (as-> url params
    (dissect params)
    (clojure.set/rename-keys params {:path :context})
    (select-keys params [:protocol :host :port :context :url :query])
    (->> params
         (map (fn [[k v]]
                (cond (and (= k :port) (string? v)) [k (parse-int v)]
                      (and (= k :query) (string? v)) [:query-params
                                                      (parse-query v)]
                      :else [k v])))
         (into {}))
    (merge (:query-params params) params)
    (dissoc params :query :query-params)))

;(parse-base-url "http://localhost:1234/ctx?enabled=yes")
;(parse-base-url "http://localhost:1234")
;(parse-base-url "http://loclahost:8883?enabled=false")
