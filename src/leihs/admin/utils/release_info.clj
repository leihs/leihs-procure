(ns leihs.admin.utils.release-info
  (:refer-clojure :exclude [str keyword])
  (:require
   [clj-yaml.core :as yaml]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [leihs.core.core :refer [keyword str presence]]))


