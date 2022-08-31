(ns leihs.admin.resources.statistics.shared
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.core.core :refer [keyword str presence]])
  (:require

    [clojure.set]
    [clojure.tools.logging :as logging]
    [leihs.core.sql :as sql]
    [logbug.debug :as debug]
    ))


(def now (sql/raw " now() "))
(def one-year-ago (sql/raw " now() - interval '1 years' "))
(def two-years-ago (sql/raw " now() - interval '2 years' "))


(def active_reservations_0m_12m_cond
  [:or
   [:and
    [:<= :reservations.start_date now]
    [:>  :reservations.start_date one-year-ago]]
   [:and
    [:<= :reservations.returned_date now]
    [:>  :reservations.returned_date one-year-ago]]])

(def active_reservations_12m_24m_cond
  [:or
   [:and
    [:<= :reservations.start_date one-year-ago]
    [:>  :reservations.start_date two-years-ago]]
   [:and
    [:<= :reservations.returned_date one-year-ago]
    [:>  :reservations.returned_date two-years-ago]]])

