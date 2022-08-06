(ns leihs.admin.state
  (:refer-clojure :exclude [str keyword])
  (:require
    [clj-yaml.core :as yaml]
    [clojure.java.io :as io]
    [honey.sql :refer [format] :rename {format sql-format}]
    [honey.sql.helpers :as sql]
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.db :as db]
    [next.jdbc :as jdbc]
    [next.jdbc.sql :refer [query] :rename {query jdbc-query}]
    [taoensso.timbre :refer [error warn info debug spy]]
    [tick.core :as tick]
    ))

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

(defn init-settings [ds]
  (-> (sql/select :external_base_url :documentation_link)
      (sql/from :settings)
      (sql/from :system_and_security_settings)
      (sql-format)
      (->> (#(jdbc-query ds % db/builder-fn-options)) first
           (swap! state* assoc :settings))))

(comment (init-settings @db/ds-next*))


(defn init [ds]
  (info "initializing global state ...")
  (init-built-info)
  (init-settings ds)
  (info "initialized state " @state*))

