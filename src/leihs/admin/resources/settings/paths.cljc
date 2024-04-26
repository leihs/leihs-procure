(ns leihs.admin.resources.settings.paths
  (:require
   [bidi.verbose :refer [branch leaf]]))

(def paths
  (branch "/settings/"
          (leaf "" :settings)
          (leaf "languages/" :languages-settings)
          (leaf "system-and-security/" :system-and-security-settings)
          (leaf "misc/" :misc-settings)
          (leaf "smtp/" :smtp-settings)
          (leaf "syssec/" :syssec-settings)))
