(ns leihs.admin.utils.release-info
  (:refer-clojure :exclude [str keyword])
  (:require 
    [leihs.admin.utils.core :refer [keyword str presence]]

    [clojure.java.io :as io] 
    [clojure.string :as str] 
    [yaml.core :as yaml]
    ))


(defn update-version-build-fn [b]
  (if (= b "$TIMESTAMP$") 
    (some-> "public/admin/build-timestamp.txt"
            io/resource 
            slurp
            str/trim)
    b))

(def leihs-admin-version
  (-> "public/admin/releases.yml"
      io/resource
      slurp
      yaml/parse-string
      :releases
      first
      (update :version_build update-version-build-fn)))


(def leihs-version
  (-> (some-> "public/admin/leihs-releases.yml"
              io/resource
              slurp
              yaml/parse-string
              :releases
              first)
      (or {:version_major 5
           :version_minor 0
           :version_patch 0
           :version_pre "PRE"
           :version_build "$TIMESTAMP$"
           :description "" })
      (update :version_build update-version-build-fn)))

