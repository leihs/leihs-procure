(ns leihs.procurement.utils.json-protocol
  (:require [cheshire core generate]
            [clj-time [coerce :as time-coerce] [format :as time-format]]
            clojure.data.json))

(defn- date-time-to-string
  [date-time]
  (time-format/unparse (time-format/formatters :date-time) date-time))

(clojure.core/extend-type org.joda.time.DateTime
  clojure.data.json/JSONWriter
    (-write [date-time out]
      (clojure.data.json/-write (date-time-to-string date-time) out)))


(clojure.core/extend-type java.sql.Timestamp
  clojure.data.json/JSONWriter
    (-write [sql-time out]
      (clojure.data.json/-write (time-coerce/from-sql-time sql-time) out)))

(clojure.core/extend-type java.util.UUID
  clojure.data.json/JSONWriter
    (-write [uuid out] (clojure.data.json/-write (. uuid toString) out)))


(cheshire.generate/add-encoder
  org.joda.time.DateTime
  (fn [date-time jsonGenerator]
    (.writeString jsonGenerator (date-time-to-string date-time))))
(defn to-json [d] (cheshire.core/generate-string d))
