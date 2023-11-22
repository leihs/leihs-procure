(ns leihs.admin.common.components.table
  (:require
   [leihs.admin.common.icons :as icons]
   [leihs.core.paths :refer [path]]
   [leihs.core.routing.front :as routing]
   [react-bootstrap :as react-bootstrap :refer [Button ButtonGroup ButtonToolbar Table]]))

(defn pagination []
  (let [hk (some-> @routing/state* :handler-key)
        route-params (or (some-> @routing/state* :route-params) {})
        query-parameters (some-> @routing/state* :query-params-raw)
        current-page (or (some-> query-parameters :page int) 1)]
    (if-not hk
      [:div "pagination not ready"]
      [:div
       (let [ppage (dec current-page)
             ppagepath (path hk route-params
                             (assoc query-parameters :page ppage))]
         [:> ButtonGroup
          [:> Button
           {:aria-label "previous"
            :data-test-id "previous"
            :variant "outline-primary"
            :disabled (< ppage 1)
            :href ppagepath}
           [icons/chevron-left]]
          [:> Button
           {:aria-label "next"
            :data-test-id "next"
            :variant "outline-primary"
            :href (path hk route-params
                        (assoc query-parameters :page (inc current-page)))}
           [icons/chevron-right]]])])))

(defn toolbar [items]
  [:> ButtonToolbar {:className "my-3"}
   [pagination]
   items])

(defn container
  [& {:keys [className header body footer actions borders]
      :or {className nil
           header nil
           body nil
           footer nil
           actions nil
           borders true}}]
  [:section
   [:<> actions]
   (when true
     [:hr.mb-0])
   [:> Table {:striped true
              :bordered false
              :borderless true
              :className (str "m-0 " className)}
    [:thead
     header]
    [:tbody
     body]
    [:tfoot
     footer]]
   (when true
     [:hr.mt-0])
   [:<> actions]])

