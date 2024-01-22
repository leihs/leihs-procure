(ns leihs.procurement.resources.users
  (:require
   [clojure.string :as clj-str]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [next.jdbc :as jdbc]
   [taoensso.timbre :refer [debug error info spy warn]]))

(defn sql-order-users
  [sqlmap]
  (sql/order-by sqlmap [[:concat :users.firstname :users.lastname :users.login :users.id]]))

(def users-base-query (-> (sql/select :users.id :users.firstname :users.lastname)
                          (sql/from :users)
                          sql-order-users))

(defn get-users
  [context args _]
  (jdbc/execute!
   (-> context
       :request
       :tx-next)
   (let [search-term (:search_term args)
         term-parts (and search-term
                         (map (fn [part] (str "%" part "%"))
                              (clj-str/split search-term #"\s+")))
         is-requester (:isRequester args)
         exclude-ids (:exclude_ids args)
         offset (:offset args)
         limit (:limit args)]
     (-> (cond-> users-base-query
           is-requester (sql/join :procurement_requesters_organizations
                                  [:= :procurement_requesters_organizations.user_id :users.id])
           term-parts (sql/where
                       (into [:and] (map (fn [x]
                                           [:ilike [:unaccent [:concat :users.firstname [:cast " " :varchar]
                                                               :users.lastname]]
                                            [:unaccent [:cast x :varchar]]]) term-parts)))
           exclude-ids (sql/where [:not-in :users.id exclude-ids])
           offset (sql/offset offset)
           limit (sql/limit limit))
         sql-format))))