(ns leihs.admin.utils.release-info
  (:refer-clojure :exclude [str keyword])
  (:require
    [clojure.java.io :as io]
    [clojure.string :as str]
    [leihs.core.core :refer [keyword str presence]]
    [clj-yaml.core :as yaml]
    ))


