(ns leihs.admin.resources.users.user.edit-image
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.admin.common.form-components :refer [input-component]]
            [leihs.admin.resources.users.user.edit-core :as edit-core :refer [data*]]
            [leihs.admin.resources.users.user.edit-image-resize :as image-resize]
            [leihs.core.core :refer [keyword presence str]]
            [leihs.core.digest :as digest]
            [reagent.core :as reagent]))

;;; image ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn update-img-digest
  "sets img_digest to the md5 hex of the concatenated img256_url and img32_url
    or to nil if both are empty; call this if either the fields :img256_url or :img32_url
    have been updated via the form; or via image drop or delete"
  [& args]
  (swap! data*
         (fn [user-data]
           (assoc user-data
                  :img_digest (some-> (str (:img256_url user-data)
                                           " "
                                           (:img32_url user-data))
                                      presence
                                      digest/md5-hex)))))

(def img-processing* (reagent/atom {}))

(defn allow-drop [e] (.preventDefault e))

(defn get-img-data [dataTransfer array-buffer-handler]
  (js/console.log (clj->js ["TODO" 'get-img-data dataTransfer]))
  (let [url (.getData dataTransfer "text/plain")]
    (js/console.log (clj->js ["URL" url]))
    (js/console.log (clj->js ["DATA" (.getData dataTransfer "text/uri-list")]))
    (js/console.log (clj->js ["DATA" (.getData dataTransfer "text/html")]))
    (js/console.log (clj->js ["ITEMS" (.-items dataTransfer)]))
    (js/console.log (clj->js ["TYPES" (.-types dataTransfer)]))))

(defn get-file-data [dataTransfer array-buffer-handler]
  (let [f (aget (.-files dataTransfer) 0)
        fname (.-name f)
        reader (js/FileReader.)]
    (set! (.-onload reader)
          (fn [e]
            (let [data (-> e .-target .-result)]
              (array-buffer-handler data))))
    (.readAsArrayBuffer reader f)))

(defn img-handler [data]
  (doseq [res [256 32]]
    (-> data
        (image-resize/resize-to-b64
         res
         :error-handler (fn [err]
                          (swap! img-processing* assoc :error err))
         :success-handler (fn [b64]
                            (swap! data* assoc (keyword (str "img" res "_url")) b64)
                            (update-img-digest))))))

(defn handle-img-drop [evt]
  (reset! img-processing* {})
  (allow-drop evt)
  (.stopPropagation evt)
  (let [data-transfer (.. evt -dataTransfer)]
    (if (< 0 (-> data-transfer .-files .-length))
      (get-file-data data-transfer img-handler)
      (get-img-data data-transfer img-handler))))

(defn handle-img-chosen [evt]
  (reset! img-processing* {})
  (get-file-data (-> evt .-target) img-handler))

(defn file-upload []
  [:div.box.mb-2
   {:style {:position :relative}
    :on-drag-over #(allow-drop %)
    :on-drop #(handle-img-drop %)
    :on-drag-enter #(allow-drop %)}
   [:div
    {:style
     {:position :relative
      :left 0
      :top 0
      :width 256
      :height 256}}
    (if-let [img-data (:img256_url @data*)]
      [:img {:src img-data
             :style {:display :block
                     :margin :auto
                     :max-width "256px"
                     :max-height "256px"
                     :opacity 0.4}}]
      [:div.bg-light
       {:style {:position :absolute
                :left 0
                :top 0
                :width "256px"
                :height "256px"}}])]
   [:div.text-center
    {:style
     {:position :absolute
      :top 0
      :width "256px"
      :height "256px"}}
    [:div.pt-2
     [:label.btn.btn-sm.btn-dark
      [:i.fas.fa-file-image]
      " Choose file "
      [:input#user-image.sr-only
       {:type :file
        :on-change handle-img-chosen}]]
     [:p "or drop file image here"]]
    [:div.text-center
     {:style {:position :absolute
              :bottom 0
              :width "100%"}}
     [:div
      (when (:img256_url @data*)
        [:p {:style {:margin-top "1em"}}
         [:a.btn.btn-sm.btn-dark
          {:href "#"
           :on-click #(swap! data* assoc :img256_url nil :img32_url nil :img_digest nil)}
          [:i.fas.fa-times] " Remove image "]])]]]])

(defn image-component [data*]
  [:div
   [:div.form-row
    [:div.col-md-4 [file-upload]]
    [:div.col-md-8
     [input-component data* [:img256_url]
      :label "Large image URL"
      :post-change update-img-digest]
     [input-component data* [:img32_url]
      :label "Small image URL"
      :post-change update-img-digest]
     [input-component data* [:img_digest]
      :label "Image digest"
      :hint [:span
             "This field is meant to set and used by syncs via the API. "
             "Some sort of digest based on the original source can hint to prevent unecessary and costly updates of image fields. "
             [:strong "Proceed judiciously when overriding this field manually!"]]]]]])
