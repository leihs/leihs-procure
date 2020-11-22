(ns leihs.admin.common.components
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.routing.front :as routing]
    [leihs.admin.utils.misc :as front-shared :refer [gravatar-url wait-component]]
    [leihs.core.icons :as icons]

    [taoensso.timbre :as logging]
    [cljs.pprint :refer [pprint]]
    ))


(defn link [inner path]
  (if (not= (:path @routing/state*) path)
    [:a {:href path} inner]
    [:span.text-info inner]))


(defn img-large-component [user]
  [:img.bg-light.user-image-256
   {:src (or (-> user :img256_url presence)
             (gravatar-url (or (-> user :email presence)
                               (-> user :secondary_email presence)
                               (-> user :id)) 256))
    :style {:max-width 256 :max-height 256}}])

(defn img-small-component [user]
  [:img.bg-light.user-image-32
   {:src (or (-> user :img32_url presence)
             (gravatar-url (or (-> user :email presence)
                               (-> user :secondary_email presence)
                               (-> user :id)) 32))
    :style {:max-width 32 :max-height 32 }}])

(defn pre-component [data]
  [:pre (with-out-str (pprint data))])
