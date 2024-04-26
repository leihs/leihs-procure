(ns leihs.admin.resources.users.choose-core
  (:require
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.admin.resources.users.user.core :refer [sql-merge-unique-user]]
   [next.jdbc.sql :refer [query] :rename {query jdbc-query}]))

(defn find-by-some-uid-query [unique-id]
  (-> (sql/select :*)
      (sql/from :users)
      (sql/where [:= nil :delegator_user_id])
      (sql-merge-unique-user unique-id)))

(defn find-user-by-some-uid! [uid tx]
  (let [user-seq (->> uid
                      find-by-some-uid-query
                      sql-format
                      (jdbc-query tx))]
    (cond
      (= 1 (count user-seq)) (first user-seq)
      (empty? user-seq) (throw
                         (ex-info "in find-user-by-some-uid! no matching user found"
                                  {:status 422 :unique-id uid}))
      (>= 2 (count user-seq)) (throw
                               (ex-info "in find-user-by-some-uid! multiple users found"
                                        {:status 422 :unique-id uid})))))
