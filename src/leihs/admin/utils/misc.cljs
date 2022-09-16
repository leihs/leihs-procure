(ns leihs.admin.utils.misc
  (:refer-clojure :exclude [str keyword])
  (:require
    [leihs.admin.common.icons :as icons]
    [leihs.admin.state :as state]
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.digest]

    ["date-fns" :as date-fns]
    [cljs.pprint :refer [pprint]]
    [goog.string :as gstring]))


; TODO stuff in this namespace should be moved removed completely


(defn humanize-datetime [ref_dt dt add-suffix]
  [:span (date-fns/formatDistance
           dt ref_dt
           (clj->js {:addSuffix add-suffix}))])

(defn humanize-datetime-component [dt & {:keys [add-suffix]
                                         :or {add-suffix true}}]
  (if-let [dt (if (string? dt) (js/Date. dt) dt)]
    [:span.datetime
     {:data-iso8601 (.toISOString dt)}
     ;[:pre (with-out-str (pprint (.toISOString dt)))]
     [humanize-datetime (:timestamp @state/global-state*) dt add-suffix]]
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
  [:h3.text-center.wait-component
   [icons/waiting]])
