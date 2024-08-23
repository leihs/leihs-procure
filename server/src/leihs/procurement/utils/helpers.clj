(ns leihs.procurement.utils.helpers
  (:require [clojure.set :refer [subset?]]
            [taoensso.timbre :refer [debug error info spy warn]])
  (:import (java.time ZoneId)
           (java.time ZonedDateTime)
           (java.time.format DateTimeFormatter)
           (java.util UUID)))

(defn submap? [m1 m2] (subset? (set m1) (set m2)))

(defn reject-keys [m ks] (reduce #(dissoc %1 %2) m ks))

(defn timestamp-to-zoneddatetime [timestamp]
  (let [instant (.toInstant timestamp)
        zone-id (ZoneId/of "Z")]
    (ZonedDateTime/ofInstant instant zone-id)))

(defn format-date [timestamp]
  (.format (timestamp-to-zoneddatetime timestamp) (DateTimeFormatter/ISO_INSTANT)))

(defn to-uuid [id] (if (instance? String id) (UUID/fromString id) id))

(defn convert-dates [entry]
  (-> entry
      (update :start_date #(if (contains? entry :start_date) (format-date %)))
      (update :end_date #(if (contains? entry :end_date) (format-date %)))
      (update :created_at #(if (contains? entry :created_at) (format-date %)))
      (update :inspection_start_date #(if (contains? entry :inspection_start_date) (format-date %)))
      (update :updated_at #(if (contains? entry :updated_at) (format-date %)))))
