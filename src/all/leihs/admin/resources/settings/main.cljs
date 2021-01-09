(ns leihs.admin.resources.settings.main
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.requests.core :as requests]
    [leihs.core.routing.front :as routing]

    [leihs.admin.common.components :as components]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.settings.breadcrumbs :as breadcrumbs]
    [leihs.admin.resources.settings.languages.breadcrumbs :as languages-breadcrumbs]
    [leihs.admin.resources.settings.misc.breadcrumbs :as misc-breadcrumbs]
    [leihs.admin.resources.settings.smtp.breadcrumbs :as smtp-breadcrumbs]
    [leihs.admin.resources.settings.syssec.breadcrumbs :as syssec-breadcrumbs]
    [leihs.admin.resources.settings.icons :as icons]
    [leihs.admin.state :as state]

    [accountant.core :as accountant]
    [cljs.core.async :as async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [reagent.core :as reagent]))


(defn page []
  [:div.settings-page
   [breadcrumbs/nav-component
    @breadcrumbs/left*
    [[languages-breadcrumbs/languages-settings-li]
     [misc-breadcrumbs/misc-settings-li]
     [smtp-breadcrumbs/smtp-settings-li]
     [syssec-breadcrumbs/syssec-settings-li]]]
   [:h1 icons/settings " Settings"]])
