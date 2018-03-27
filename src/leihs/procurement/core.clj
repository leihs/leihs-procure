(ns leihs.procurement.core
  (:require [leihs.procurement.schema :as s]
            [com.walmartlabs.lacinia :as lacinia]))

(def schema (s/load-schema))

(defn q [query-string]
  (lacinia/execute schema query-string nil nil))

; (require '[leihs.procurement.core :reload-all true])

; (q "{ request_by_id(id: \"'91805c8c-0f47-45f1-bcce-b11da5427294'\") { id article_name }}")
