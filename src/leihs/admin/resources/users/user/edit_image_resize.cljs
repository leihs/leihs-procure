(ns leihs.admin.resources.users.user.edit-image-resize
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    ["@jimp/custom"]
    ["jimp" :as Jimp]
    [cljs.core.async :as async]
    [cljs.core.async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.users.user.core :as core :refer [user-id*]]
    [leihs.admin.resources.users.user.edit-core :as edit-core :refer [data*]]
    [leihs.admin.state :as state]
    [leihs.admin.utils.misc :as front-shared :refer [wait-component]]
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.routing.front :as routing]
    [reagent.core :as reagent]
    [taoensso.timbre :as logging]
    ))

(defn resize-to-b64
  [data max-dimension & {:keys [error-handler success-handler]
                         :or {error-handler #()
                              success-handler #()}}]
  (.read Jimp data
         (fn [err ^js img]
           (if err
             (error-handler err)
             (do (doto img
                   (.resize max-dimension max-dimension)
                   (.quality 80))
                 (.getBase64 img "image/jpeg"
                             (fn [err b64]
                               (if err
                                 (error-handler err)
                                 (success-handler b64)))))))))
