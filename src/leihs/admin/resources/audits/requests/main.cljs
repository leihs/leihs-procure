(ns leihs.admin.resources.audits.requests.main
  (:refer-clojure :exclude [str keyword])
  (:require
   [accountant.core :as accountant]
   [cljs.core.async :as async :refer [go timeout <!]]
   [cljs.pprint :refer [pprint]]
   [clojure.set :refer [rename-keys]]
   [clojure.string :as string]
   [leihs.admin.common.components :as components]
   [leihs.admin.common.components.filter :as filter]
   [leihs.admin.common.components.table :as table]
   [leihs.admin.common.form-components :as form-components]
   [leihs.admin.common.http-client.core :as http]
   [leihs.admin.common.icons :as icons]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.audits.core :as audits]
   [leihs.admin.resources.audits.requests.breadcrumbs :as breadcrumbs]
   [leihs.admin.resources.audits.requests.shared :refer [default-query-params]]
   [leihs.admin.resources.users.user.core :as user]
   [leihs.admin.state :as state]
   [leihs.admin.utils.clipboard :as clipboard]
   [leihs.admin.utils.misc :as front-shared :refer [wait-component]]
   [leihs.core.core :refer [presence]]
   [leihs.core.routing.front :as routing]
   [reagent.core :as reagent]
   [taoensso.timbre :refer [warn]]))

(def requests* (reagent/atom {}))

(defn page-path-for-query-params [query-params]
  (path (:handler-key @routing/state*)
        (:route-params @routing/state*)
        (merge default-query-params (:query-params-raw @routing/state*) query-params)))

(defn fetch [& _]
  (let [chan (async/chan)
        req (http/request {:chan chan})]
    (go (let [resp (<! chan)]
          (when (< (:status resp) 300)
            (let [url (:url req)]
              (swap! requests* assoc url (-> resp :body :requests))
              ; after some time reload or clean cached
              (go (<! (timeout (* 3 60 1000)))
                  (if (= url (:route @routing/state*))
                    (fetch)
                    (swap! requests* dissoc url)))))))))

(defn responsible-user-choose-component []
  [:div.input-group-prepend
   [:a.btn.btn-primary
    {:tab-index form-components/TAB-INDEX
     :href (path :users-choose {}
                 {:return-to (:route @routing/state*)})}
    [:i.fas.fa-rotate-90.fa-hand-pointer.px-2]
    " Choose "]])

(defn method-filter-component []
  [:div.form-group.m-2
   [:label {:for :method}
    [:span "Method" [:small.text_monspache " (method)"]]]
   [:select#method.form-control
    {:value (:method (merge default-query-params
                            (:query-params-raw @routing/state*)))
     :on-change (fn [e]
                  (let [val (or (-> e .-target .-value presence) "")]
                    (accountant/navigate! (page-path-for-query-params
                                           {:page 1
                                            :method val}))))}
    (for [[n v] (->> ["" "DELETE" "GET" "PATCH" "POST" "PUT"]
                     (map (fn [op] [op op])))]
      ^{:key n} [:option {:value v} n])]])

(defn filter-component []
  [filter/container
   [:<>
    [filter/delayed-query-params-input-component
     :label "Requester"
     :query-params-key :user-uid
     :prepend responsible-user-choose-component]
    [:div.col-md-2 [method-filter-component]]
    [:div.col-md-2 [filter/form-per-page]]
    [:div.col-md-2 [filter/reset :default-query-params default-query-params]]]])

;;; table ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn thead-component [hds]
  [:tr
   [:th {:key :timestamp} "Timestamp"]
   [:th {:key :txid} "TX ID"]
   [:th {:key :http-uid} "HTTP-UID"]
   [:th {:key :method} "Method"]
   [:th {:key :status} "Status"]
   [:th {:key :path} "Handler | Path"]
   [:th {:key :requester} "Requester"]
   [:th {:key :actions}]
   (for [[idx hd] (map-indexed vector hds)]
     ^{:key idx} [hd])])

(defn td-requester [request]
  [:td.requester {:key :requester}
   (if-let [user-uid (-> request
                         (select-keys [:requester_id :requester_email :requester_login])
                         (rename-keys {:requester_id :id
                                       :requester_email :email
                                       :requester_login :login})
                         user/some-uid
                         presence)]
     [:span user-uid]
     [:span "-"])])

(defn td-path-handler [request]
  [:td.path-handler
   (let [path (->> request :path)]
     [:<>
      (if-let [params (routing/match-path path)]
        [:span (:handler params)]
        [components/truncated-id-component path
         :key :path :max-length 24 :copy-to-clipboard false])
      " " [clipboard/button-tiny path]])])

(defn row-component [request tds]
  (warn 'request request)
  [:tr.request
   {:key (:txid request)}
   [:td.text-monospace.timestamp {:key :timestamp} (:request_timestamp request)]
   [:td [components/truncated-id-component (:txid request) :key :txid]]
   [:td [components/truncated-id-component (:http_uid request) :key :http-uid]]
   [:td.text-monospace {:key :method} (->> request :method string/upper-case)]
   [:td.text-monospace {:key :status} (->> request :response_status)]
   [td-path-handler request]
   [td-requester request]
   [:td.actions
    [:a {:href (path :audited-request {:request-id (:id request)})}
     [:span [icons/view] " Request "]]]
   (for [[idx col] (map-indexed vector tds)]
     ^{:key idx} [col request])])

(defn table-component [requests hds tds]
  [table/container {:actions [table/toolbar]
                    :header
                    [thead-component hds]
                    :body
                    (doall (for [request requests]
                             ^{:key (:txid request)} [row-component request tds]))}])

(defn main-component []
  [:div.audited-requests-main
   [routing/hidden-state-component
    {:did-change fetch}]
   (if-not (contains? @requests* @routing/current-url*)
     [wait-component]
     (if-let [requests (-> @requests* (get  @routing/current-url* {}) seq)]
       [table-component requests]
       [:div.alert.alert-warning.text-center "No (more) audited-requests found."]))])

(defn debug-component []
  (when @state/debug?*
    [:div.debug
     [:hr]
     [:div.requests*
      [:h3 "@data"]
      [:pre (with-out-str (pprint @requests*))]]]))

(defn page []
  [:article.audited-requests-page
   [:header.my-5
    [:h1 [icons/code-pull-request] " Audited Requests"]]
   [:section
    [filter-component]
    [main-component]
    [debug-component]]])
