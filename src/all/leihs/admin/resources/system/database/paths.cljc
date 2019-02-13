(ns leihs.admin.resources.system.database.paths
  (:refer-clojure :exclude [str keyword])

  (:require
    [leihs.core.core :refer [keyword str presence]]

    [bidi.verbose :refer [branch param leaf]]
    ))

(def paths
  (branch "/database/"
          (leaf "" :database)
          (branch "audits"
                  (leaf "" :database-audits)
                  (branch "/before/"
                          (param :before-date)
                          (leaf "" :database-audits-before)))))
