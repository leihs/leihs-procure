(ns leihs.procurement.status
  (:refer-clojure :exclude [str keyword])
  (:require [cheshire.core :refer [generate-string]]
            [clojure.tools.logging :as logging]
            [compojure.core :as cpj]
            [hiccup.page :refer [html5]]
            [leihs.procurement.paths :refer [path]]
            [leihs.procurement.utils.ds :as ds])
  (:import com.codahale.metrics.health.HealthCheckRegistry
           humanize.Humanize))

;;; health checks
;;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce health-check-registry* (atom nil))

(defn HealthCheckResult->m
  [r]
  {:healthy? (.isHealthy r), :message (.getMessage r), :error (.getError r)})

;(.getNames @health-check-registry*)
;(.runHealthChecks @health-check-registry*)

(defn health-checks
  []
  (some->> @health-check-registry*
           .runHealthChecks
           (map (fn [[n r]] [n
                             (-> r
                                 HealthCheckResult->m)]))
           (into {})))

;;; memory
;;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn check-memory-usage
  []
  ;(System/gc)
  (let [rt (Runtime/getRuntime)
        max-mem (.maxMemory rt)
        allocated-mem (.totalMemory rt)
        free (.freeMemory rt)
        used (- allocated-mem free)
        usage (double (/ used max-mem))
        ok? (and (< usage 0.95) (> free))
        stats {:ok? ok?,
               :max (Humanize/binaryPrefix max-mem),
               :allocated (Humanize/binaryPrefix allocated-mem),
               :used (Humanize/binaryPrefix used),
               :usage usage}]
    (when-not ok? (logging/fatal stats))
    stats))

;;; main
;;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn head
  []
  [:head [:meta {:charset "utf-8"}]
   [:meta
    {:name "viewport",
     :content "width=device-width, initial-scale=1, shrink-to-fit=no"}]])

(defn status-handler
  [request]
  (let [memory-status (check-memory-usage)
        health-checks (health-checks)
        body (generate-string {:memory memory-status,
                               :db-pool (ds/status),
                               :health-checks health-checks}
                              {:pretty true})]
    {:status (if (and (->> [memory-status]
                           (map :ok?)
                           (every? true?))
                      (->> health-checks
                           (map second)
                           (map :healthy?)
                           (apply true?)))
               200
               900),
     :body (html5 (head) [:body [:pre body]])}))

(defn wrap
  [default-handler]
  (fn [request]
    (if (and (= (:handler-key request) :status)
             (= (-> request
                    :accept
                    :mime)
                :json))
      (status-handler request)
      (default-handler request))))

(def routes (cpj/routes (cpj/GET (path :status) [] #'status-handler)))

(defn init
  []
  (reset! health-check-registry* (HealthCheckRegistry.))
  {:health-check-registry @health-check-registry*})

;#### debug ###################################################################
;(logging-config/set-logger! :level :debug)
;(logging-config/set-logger! :level :info)
;(debug/debug-ns 'cider-ci.utils.shutdown)
;(debug/debug-ns *ns*)
