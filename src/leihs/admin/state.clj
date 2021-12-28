(ns leihs.admin.state
  (:refer-clojure :exclude [str keyword])
  (:require
    [clj-yaml.core :as yaml]
    [clojure.java.io :as io]
    [leihs.core.core :refer [keyword str presence]]
    [tick.core :as tick]
    [taoensso.timbre :as logging]))

(defonce state* (atom {}))

(defn init-built-info []
  (let [built-info (or (some-> "built-info.yml"
                               io/resource
                               slurp
                               yaml/parse-string)
                       {})]
    (swap! state* assoc :built-info built-info)
    (swap! state* update-in [:built-info :timestamp]
           #(or % (str(tick/now))))))


(defn init []
  (logging/info "initializing global state ...")
  (init-built-info)
  (logging/info "initialized state " @state*))

