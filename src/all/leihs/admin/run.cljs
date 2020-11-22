(ns leihs.admin.run
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    )
  (:require
    [leihs.admin.html :as html]
    [leihs.admin.routes :as routes]
    [leihs.admin.paths :as paths]
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.user.front :refer [load-user-data-from-dom]]

    [clojure.string :as str]
    [clojure.pprint :refer [pprint]]

    [reagent.core :as reagent]
    [accountant.core :as accountant]
    ))

(defn init! []
  (load-user-data-from-dom)
  (routes/init)
  (html/mount))
