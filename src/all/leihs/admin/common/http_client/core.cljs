(ns leihs.admin.common.http-client.core
  (:refer-clojure :exclude [str keyword send-off])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.anti-csrf.front :as anti-csrf]
    [leihs.core.core :refer [str keyword deep-merge presence]]
    [leihs.core.constants :as constants]
    [leihs.core.routing.front :as routing]

    [cljs-http.client :as http-client]
    [cljs-uuid-utils.core :as uuid]
    [cljs.core.async :as async :refer [timeout]]
    [clojure.pprint :refer [pprint]]
    [clojure.string :as str]
    [goog.string :as gstring]
    [goog.string.format]
    [reagent.core :as reagent]
    [taoensso.timbre :as logging]
    ))


; TODO
(def base-delay* (reagent/atom 1000))

(defonce requests* (reagent/atom {}))

(defn dismiss [request-id]
  (swap! requests* dissoc request-id))

(defn set-defaults [data]
  (-> data
      (update :id #(uuid/uuid-string (uuid/make-random-uuid)))
      (update :timestamp #(js/Date.))
      (update :method #(or % :get))
      (update :url #(or % (-> @routing/state* :url)))
      (update :delay #(+ (or % 0) @base-delay*))
      (update-in [:headers "accept"]
                 #(or % "application/json"))
      (update-in [:headers constants/ANTI_CSRF_TOKEN_HEADER_NAME]
                 #(or % (anti-csrf/anti-csrf-token)))
      (update :modal-on-response-error #(if-not (nil? %) % true))
      (as-> data
        (update data :modal-on-request
                #(if-not (nil? %) %
                   (if (constants/HTTP_SAVE_METHODS (:method data))
                     false true)))
        (update data :modal-on-response-success
                #(if-not (nil? %) %
                   (:modal-on-request data))))))

(defn request
  ([] (request {}))
  ([data]
   (let [req (set-defaults data)
         id (:id req)]
     (swap! requests* assoc id req)
     (go (<! (timeout (:delay req)))
         (let [resp (<! (http-client/request
                          (select-keys req [:url :method :headers :json-params :body])))]
           (when (:success resp)
             (if (:modal-on-response-success req)
               (go (<! (timeout 1000))
                   (dismiss id))
               (dismiss id)))
           (when (get @requests* id)
             (swap! requests* assoc-in [id :response] resp))
           (when-let [chan (:chan req)] (>! chan resp))))
     req)))


(defn filter-success [response]
  (when (:success response) response))

;;; UI ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn wait-component [req]
  [:div.wait-component
   {:style { :opacity 0.4}}
   [:div.text-center
    [:i.fas.fa-spinner.fa-spin.fa-5x]]
   [:div.text-center
    {:style {}}
    "Wait for " (-> req :method str str/upper-case)
    " " (:url req)]])

(defn error-component [resp req]
  [:<>
   (let [status (:status resp)]
     [:div.alert
      {:class (if (< status 500)
                "alert-warning"
                "alert-danger")}
      [:h3.alert-heading
       [:span "Request Error - Status "
        [:span.text-monospace status]]]
      [:hr]
      [:div.request
       [:pre  (-> req :method str str/upper-case) " " (:url req)]]
      (when-let [body (-> resp :body presence)]
        [:div.body [:pre body]])
      [:hr]
      (when (>= status 500)
        [:div
         [:span "Please try to reload this page with the reload button of your browser. "]
         [:span "Contact your administrator or file a bug report if the problem persists."]])])])

(defn request-response-component [req-opts inner]
  (let [req* (reagent/atom nil)
        resp* (reagent/atom nil)]
    (fn [req-opts inner]
      [:div.request-response-component
       [routing/hidden-state-component
        {:did-mount (fn [& _]
                      (if (:chan req-opts)
                        (logging/error ":chan may not be set for managed request-response-component")
                        (let [chan (async/chan)]
                          (reset! req* (request (assoc req-opts
                                                       :chan chan
                                                       :modal-on-request false
                                                       :modal-on-response-error false
                                                       :modal-on-response-success false)))
                          (go (let [resp (<! chan)]
                                (reset! resp* resp))))))}]
       (if-let [resp @resp*]
         (if (>= (:status resp) 400)
           [error-component resp @req*]
           [inner (:body resp)])
         [wait-component @req*])])))

