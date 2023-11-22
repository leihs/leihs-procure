(ns leihs.admin.resources.audits.main
  (:refer-clojure :exclude [str keyword])
  (:require-macros
   [cljs.core.async.macros :refer [go]]
   [reagent.ratom :as ratom :refer [reaction]])
  (:require
   [leihs.admin.resources.audits.breadcrumbs :as breadcrumbs]
   [leihs.admin.resources.audits.changes.breadcrumbs :as changes-breadcrumbs]
   [leihs.admin.resources.audits.core :as audits]
   [leihs.admin.resources.audits.requests.breadcrumbs :as requests-breadcrumbs]))

(defn page []
  [:div.audits-page
   [[changes-breadcrumbs/changes-li]
    [requests-breadcrumbs/requests-li]]
   [:h1 audits/icon-audits "Audits"]])
