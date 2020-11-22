(ns leihs.admin.resources.users.choose-core
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.core.core :refer [keyword str presence]])
  (:require
    [leihs.core.sql :as sql]

    [leihs.admin.utils.regex :refer [uuid-pattern]]

    [clojure.java.jdbc :as jdbc]

    [clj-logging-config.log4j :as logging-config]
    [clojure.tools.logging :as logging]
    [logbug.debug :as debug]
    ))

(defn find-by-some-uid-query [unique-id]
  (-> (sql/select :*)
      (sql/from :users)
      (sql/merge-where [:= nil :delegator_user_id])
      (sql/merge-where
        [:or
         (if (clojure.string/includes? unique-id "@" )
           [:= (sql/call :lower :users.email) (sql/call :lower unique-id)]
           [:= :users.login unique-id])
         (when (re-matches uuid-pattern unique-id)
           [:= :users.id unique-id])])))

(defn find-user-by-some-uid! [uid tx]
  (let [user-seq (->> uid
                      find-by-some-uid-query
                      sql/format
                      (jdbc/query tx))]
    (cond
      (= 1 (count user-seq)) (first user-seq)
      (empty? user-seq) (throw
                          (ex-info "in find-user-by-some-uid! no matching user found"
                                   {:status 422 :unique-id uid}))
      (>= 2 (count user-seq)) (throw
                                (ex-info "in find-user-by-some-uid! multiple users found"
                                         {:status 422 :unique-id uid})))))


