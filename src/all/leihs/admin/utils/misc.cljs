(ns leihs.admin.utils.misc
  (:refer-clojure :exclude [str keyword])
  (:require
    [leihs.admin.state :as state]
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.digest]

    [cljsjs.moment]
    [goog.string :as gstring]))


; TODO stuff in this namespace should be moved removed completely


; timestuff, js/moment will be replaced when we switch the build system

(defn humanize-datetime [ref_dt dt]
  (.to (js/moment) dt))

(defn humanize-datetime-component [dt]
  (if-let [dt (if (string? dt) (js/moment dt) dt)]
    [:span.datetime
     {:data-iso8601 (.format dt)}
     ;[:pre (with-out-str (pprint dt))]
     [humanize-datetime (:timestamp @state/global-state*) dt]]
    [:span "NULL"]))



; gravatar will be removed ; there is a ticked already

(defn gravatar-url
  ([email]
   (gravatar-url email 32))
  ([email size]
   (if-not (presence email)
     (gstring/format
       "https://www.gravatar.com/avatar/?s=%d&d=blank" size)
     (let [md5 (->> email
                    clojure.string/trim
                    clojure.string/lower-case
                    leihs.core.digest/md5-hex)]
       (gstring/format
         "https://www.gravatar.com/avatar/%s?s=%d&d=retro"
         md5 size)))))


; the following should be moved to common.components

(defn wait-component []
  [:div.text-center.wait-component
   [:i.fas.fa-spinner.fa-spin.fa-5x]
   [:span.sr-only "Please wait"]])
