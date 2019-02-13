(ns leihs.admin.resources.system.paths
  (:refer-clojure :exclude [str keyword])
  (:require
    [leihs.core.core :refer [keyword str presence]]

    [leihs.admin.resources.system.authentication-systems.paths :as authentication-systems]
    [leihs.admin.resources.system.database.paths :as database-paths]
    [leihs.admin.resources.system.system-admins.paths :as system-admins-paths]

    [bidi.verbose :refer [branch param leaf]]

    ))

(def paths
  (branch "/system"
          (leaf "/" :system)
          database-paths/paths
          authentication-systems/paths
          system-admins-paths/paths))
