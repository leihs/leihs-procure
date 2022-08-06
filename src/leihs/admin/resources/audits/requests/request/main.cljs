(ns leihs.admin.resources.audits.requests.request.main
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.routing.front :as routing]
    [leihs.admin.common.icons :as icons]

    [leihs.admin.common.components :as components]
    [leihs.admin.common.form-components :as form-components]
    [leihs.admin.common.http-client.core :as http]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.audits.changes.change.main :as change]
    [leihs.admin.resources.audits.core :as audits]
    [leihs.admin.resources.audits.requests.request.breadcrumbs :as breadcrumbs]
    [leihs.admin.resources.audits.requests.shared :refer [default-query-params]]
    [leihs.admin.resources.users.user.core :as user]
    [leihs.admin.state :as state]
    [leihs.admin.utils.clipboard :as clipboard]
    [leihs.admin.utils.misc :as front-shared :refer [wait-component]]

    [clojure.string :as str]
    [accountant.core :as accountant]
    [cljs.core.async :as async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [reagent.core :as reagent]
    [taoensso.timbre :as logging]))


(def request-id* (reaction (or (-> @routing/state* :route-params :request-id) ":request-id")))

(defonce changes-index* (reagent/atom {}))

(def requester-id* (reagent/atom nil))

(defn fetch-changes-index [& _]
  (reset! changes-index* {})
  (let [url (path :audited-changes {} {:request-id @request-id*})
        chan (async/chan)
        req (http/request {:chan chan :url url})]
    (go (let [resp (<! chan)]
          (when (< (:status resp) 300)
            (reset! changes-index* (-> resp :body :changes)))))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn inner-value-component [text]
  [:<>
   [:div {:style {:position :absolute :right "1em" :bottom 0}}
    [clipboard/button-tiny text]]
   [:pre text]])

(defn generic-value-component [v]
  [:<>
   (let [text (if (string? v ) v (with-out-str (pprint v)))]
     [inner-value-component text])])

(defn path-value-component [path]
  [:<>
   (if-let [params (routing/match-path path)]
     [:ol.list-unstyled.list-group
      ^{:key :handler} [:li.list-group-item
                        [:dl.row
                         [:dd.col-sm-3 "handler"]
                         [:dd.col-sm-9 (:handler params)]]]
      (for [[k v] (:route-params params)]
        ^{:key k} [:li.list-group-item
                   [:dl.row
                    [:dd.col-sm-3 k]
                    [:dd.col-sm-9 [inner-value-component v]]]])]
     [inner-value-component path])])

(defn request-component [request]
  [:div.request
   [routing/hidden-state-component
    :did-mount #(reset! requester-id* (:requester_id request))]
   [:h3 "Request / Response "]
   [:ol.list-unstyled.list-group
    (for [[i [k v]] (map-indexed (fn [i v] [i v]) request)]
      ^{:key k} [:li.list-group-item.p-2
                 {:class (if (odd? i) "list-group-item-secondary" "")}
                 [:dl.row.my-1
                  [:dd.col-sm-3 k]
                  [:dd.col-sm-9
                   (case (keyword k)
                     :path [path-value-component v]
                     [generic-value-component v])]]])]])

(defn request-fetch-component []
  [:div.request-outer.p-2.my-5.bg-light
   {:key :request}
   [http/request-response-component
    {:url (path :audited-request {:request-id @request-id*})}
    request-component]])



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn requester-inner-component [user]
  [:div
   [:h3 "Requester"]
   [:a {:href (path :user {:user-id (:id user)})}
    [:ul.list-unstyled
     (for [prop (user/fullname-some-uid-seq user)]
       ^{:key prop} [:li [:span prop]])]]])

(defn requester-fetch-component [requester-id]
  [http/request-response-component
   {:url (path :user {:user-id requester-id})}
   requester-inner-component])

(defn requester-component []
  [:div.requester-outer.p-2.my-5.bg-light
   (when @requester-id*
     [requester-fetch-component @requester-id*])])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn changes-component []
  [:div.changes
   {:key :changes}
   (for [id (->> @changes-index* (map :id))]
     ^{:key id} [:div.p-2.my-5.bg-light
                 [change/main-component id]])])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn debug-component []
  (when @state/debug?*
    [:div
     [:hr]
     [:div.data*
      [:h3 "@changes-index*"]
      [:pre (with-out-str (pprint @changes-index*))]]]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn page []
  [:div.audited-request
   [routing/hidden-state-component
    {:did-mount (fn [& _]
                  (reset! requester-id* nil)
                  (reset! changes-index* {})
                  (fetch-changes-index))}]
   [breadcrumbs/nav-component
    @breadcrumbs/left* []]
   [:h1 audits/icon-request " Audited-Request "]
   [request-fetch-component]
   [requester-component]
   [changes-component]
   [debug-component]])
