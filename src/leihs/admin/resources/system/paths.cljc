(ns leihs.admin.resources.system.paths
  (:refer-clojure :exclude [str keyword])
  (:require
   [bidi.verbose :refer [branch param leaf]]
   [leihs.admin.resources.system.authentication-systems.paths :as authentication-systems]
   [leihs.core.core :refer [keyword str presence]]))

(def paths
  (branch "/system"
          (leaf "/" :system)
          authentication-systems/paths))
