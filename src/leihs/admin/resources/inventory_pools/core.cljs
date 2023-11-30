(ns leihs.admin.resources.inventory-pools.core
  (:refer-clojure :exclude [str keyword])
  (:require-macros
   [cljs.core.async.macros :refer [go]]
   [reagent.ratom :as ratom :refer [reaction]])
  (:require
   [leihs.core.core :refer [keyword str presence]]
   [leihs.core.routing.front :as routing]))



