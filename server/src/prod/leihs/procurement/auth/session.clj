(ns leihs.procurement.auth.session
  (:require [leihs.core.auth.session :as session]))

(def wrap session/wrap-authenticate)
