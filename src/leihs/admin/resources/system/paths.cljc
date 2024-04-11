(ns leihs.admin.resources.system.paths
  (:require
   [bidi.verbose :refer [branch]]
   [leihs.admin.resources.system.authentication-systems.paths :as authentication-systems]))

(def paths
  (branch "/system"
          authentication-systems/paths))
