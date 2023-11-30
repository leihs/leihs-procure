(ns leihs.admin.resources.settings.paths
  (:refer-clojure :exclude [str keyword])
  (:require
   [bidi.verbose :refer [branch param leaf]]

   [leihs.core.core :refer [keyword str presence]]))

(def paths
  (branch "/settings/"
          (leaf "" :settings)
          (leaf "languages/" :languages-settings)
          (leaf "system-and-security/" :system-and-security-settings)
          (leaf "misc/" :misc-settings)
          (leaf "smtp/" :smtp-settings)
          (leaf "syssec/" :syssec-settings)))
