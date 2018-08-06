(ns leihs.procurement.utils.ds
  (:refer-clojure :exclude [str keyword])
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log]
            [hikari-cp.core :as hikari]
            [leihs.procurement.utils.core :refer [presence str]]
            [pg-types.all])
  (:import com.codahale.metrics.MetricRegistry))

(defonce metric-registry* (atom nil))

(defn Timer->map
  [t]
  {:count (.getCount t),
   :mean-reate (.getMeanRate t),
   :one-minute-rate (.getOneMinuteRate t),
   :five-minute-rate (.getFiveMinuteRate t),
   :fifteen-minute-rate (.getFifteenMinuteRate t)})

(defn status
  []
  {:gauges (->> @metric-registry*
                .getGauges
                (map (fn [[n g]] [n (.getValue g)]))
                (into {})),
   :timers (->> @metric-registry*
                .getTimers
                (map (fn [[n t]] [n (Timer->map t)]))
                (into {}))})

(defonce ds (atom nil))
(defonce ds-without-pooler (atom nil))
(defn get-ds [] @ds)
(defn get-ds-without-pooler [] @ds-without-pooler)

(defn wrap-tx
  [handler]
  (fn [request]
    ; NOTE: for images we have to use a datasource without pooler as somewhere
    ; there
    ; is a bug which leads to hanging of the server on all subsequent requests
    ; (let [ds (if (= (:handler-key request) :image) @ds-without-pooler @ds)]
    ;
    ; FIXME: using ds without pooler for every handelr for the time being as
    ; the problem with hanging server still persists
    (let [ds @ds-without-pooler]
      (jdbc/with-db-transaction
        [tx ds]
        (try
          (let [resp (handler (assoc request :tx tx))
                status (:status resp)]
            (cond (and status (>= status 400))
                    (do (log/warn
                          "Rolling back transaction because error status "
                          status)
                        (jdbc/db-set-rollback-only! tx))
                  (:graphql-error resp)
                    (do (log/warn
                          "Rolling back transaction because of graphql error")
                        (jdbc/db-set-rollback-only! tx)))
            resp)
          (catch Throwable th
            (log/warn "Rolling back transaction because of " th)
            (jdbc/db-set-rollback-only! tx)
            (throw th)))))))

(defn get-database-url
  [params]
  (str "postgresql://"
       (if-let [username (:username params)]
         (if-let [password (:password params)]
           (str username ":" password "@")
           (str username "@")))
       (:host params)
       (when-let [port (-> params
                           :port
                           presence)]
         (str ":" port))
       "/"
       (:database params)))

(defn close-ds!
  []
  (log/info "Closing db pool ...")
  (-> @ds
      :datasource
      hikari/close-datasource)
  (reset! ds nil)
  (log/info "Closing db pool done."))

(defn close-ds-without-pooler!
  []
  (log/info "Closing ds without pooler ...")
  (reset! ds-without-pooler nil)
  (log/info "Closing ds without pooler done."))

(defn initialize-ds!
  [params health-check-registry]
  (let [ds-spec {:auto-commit true,
                 :read-only false,
                 :connection-timeout 30000,
                 :validation-timeout 5000,
                 :idle-timeout 600000,
                 :max-lifetime (* 3 60 60 1000),
                 :minimum-idle 10,
                 :maximum-pool-size (-> params
                                        :max-pool-size
                                        presence
                                        (or 5)),
                 :pool-name "db-pool",
                 :adapter "postgresql",
                 :username (:username params),
                 :password (:password params),
                 :database-name (:database params),
                 :server-name (:host params),
                 :port-number (:port params),
                 :register-mbeans false,
                 :metric-registry @metric-registry*,
                 :health-check-registry health-check-registry}]
    (log/info "Initializing datasource with pooler ...")
    (log/info ds-spec)
    (reset! ds {:datasource (hikari/make-datasource ds-spec)})
    (log/info "Initializing datasource with pooler done.")))

(defn initialize-ds-without-pooler!
  [params]
  (log/info "Initializing datasource without pooler ...")
  (let [url (get-database-url params)]
    (log/info {:url url})
    (reset! ds-without-pooler url))
  (log/info "Initializing datasource without pooler done."))

(defn init
  [params health-check-registry]
  (reset! metric-registry* (MetricRegistry.))
  (when @ds (close-ds!))
  (when @ds-without-pooler (close-ds-without-pooler!))
  (initialize-ds! params health-check-registry)
  (initialize-ds-without-pooler! params)
  @ds)

;;### Debug
;;####################################################################
;(debug/debug-ns *ns*)
;(logging-config/set-logger! :level :debug)
;(logging-config/set-logger! :level :info)
