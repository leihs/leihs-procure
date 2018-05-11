(ns leihs.procurement.utils.url.core
  (:refer-clojure :exclude [str keyword encode decode])
  (:require [leihs.procurement.utils.url.shared :as shared]))

(def encode shared/encode)
(def decode shared/decode)
