(ns leihs.admin.common.components
  (:refer-clojure :exclude [str keyword])
  (:require [clojure.string :as string]
            [leihs.admin.common.icons :as icons]
            [leihs.admin.utils.clipboard :as clipboard]
            [leihs.admin.utils.misc :as front-shared :refer [gravatar-url]]
            [leihs.core.core :refer [keyword presence str]]
            [leihs.core.routing.front :as routing]))

(defn link [inner path]
  (if (not= (:path @routing/state*) path)
    [:a {:href path} inner]
    [:<> inner]))

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
    :style {:max-width 32 :max-height 32}}])

(defn truncated-id-component
  [id & {:keys [key copy-to-clipboard max-length]
         :or {key :id
              copy-to-clipboard true
              max-length 5}}]
  [:<> (if-not (presence id)
         [:span "-"]
         [:span
          {:key key
           :class (str key)
           (keyword (str  "data-" key)) id
           :style {:white-space :nowrap}}
          [:span.text-monospace
           (if (> (count id) max-length)
             (str (->> id (take max-length) string/join) "\u2026")
             id)]
          (when copy-to-clipboard
            [:sup " " [clipboard/button-tiny id]])])])

(defn toggle-component [bool]
  (if bool [icons/toggle-on] [icons/toggle-off]))
