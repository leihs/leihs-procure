(ns leihs.procurement.graphql.scalars
  (:require
    [java-time]
    [taoensso.timbre :refer [debug error info spy warn]])
  (:import
    (java.sql Timestamp)
    (java.time OffsetDateTime)
    (java.util UUID)))

(defn int-parse [x]
  (try
    (if (number? x) x (Integer/parseInt x))
    (catch Throwable _
      nil)))

(defn parse-timestamp-with-timezone [timestamp-str]
  (OffsetDateTime/parse timestamp-str))

(defn offsetdatetime-to-sqltimestamp [offset-datetime]
  (Timestamp/from (.toInstant offset-datetime)))

(defn timestamptz-parse [x]
  (try (->> x
            parse-timestamp-with-timezone
            offsetdatetime-to-sqltimestamp)
       (catch Throwable _
         nil)))

(def scalars
  {:uuid-parse #(UUID/fromString %)
   :uuid-serialize str
   :int-parse int-parse
   :int-serialize identity
   :timestamptz-parse timestamptz-parse
   :timestamptz-serialize str})
