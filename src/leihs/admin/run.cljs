(ns leihs.admin.run
  (:refer-clojure :exclude [str keyword])
  (:require-macros
   [reagent.ratom :as ratom :refer [reaction]])
  (:require
    ;[leihs.admin.paths :as paths]
   [accountant.core :as accountant]
   [clojure.pprint :refer [pprint]]
   [clojure.string :as str]
   [leihs.admin.html :as html]
   [leihs.admin.routes :as routes]
   [leihs.core.core :refer [keyword str presence]]
   [leihs.core.user.front :refer [load-user-data-from-dom]]
   [reagent.core :as reagent]
   [taoensso.timbre :refer [debug info warn error]]))

(defn init! []
  (info  "initializing")
  (load-user-data-from-dom)
  (routes/init)
  (html/mount))
