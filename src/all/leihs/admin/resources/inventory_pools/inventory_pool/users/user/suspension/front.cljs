(ns leihs.admin.resources.inventory-pools.inventory-pool.users.user.suspension.front
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
     [leihs.core.core :refer [keyword str presence]]
     [leihs.core.requests.core :as requests]
     [leihs.core.routing.front :as routing]
     [leihs.core.icons :as icons]

     [leihs.admin.front.shared :refer [wait-component]]
     [leihs.admin.front.breadcrumbs :as breadcrumbs]
     [leihs.admin.front.components :as components :refer [field-component checkbox-component]]
     [leihs.admin.front.components :as components]
     [leihs.admin.front.state :as state]
     [leihs.admin.paths :as paths :refer [path]]
     [leihs.admin.resources.inventory-pools.inventory-pool.front :as inventory-pool :refer [inventory-pool-id*]]
     [leihs.admin.resources.user.front.shared :as user :refer [user-id* user-data*]]
     [leihs.admin.utils.regex :as regex]

     [accountant.core :as accountant]
     [cljs.core.async :as async]
     [cljs.pprint :refer [pprint]]
     [reagent.core :as reagent]))



(defonce data* (reagent/atom nil))

(def suspended-until*
  (reaction
    (some-> @data* :suspended_until presence)))

(def suspended?*
  (reaction
    (if-not @suspended-until*
      false
      (if (.isAfter
            (.add (js/moment @suspended-until*) 1 "days")
            (:timestamp @state/global-state*))
        true
        false))))


(def edit-mode?*
  (reaction
    (= (-> @routing/state* :handler-key) :inventory-pool-user-suspension)))

(defn debug-component []
  (when (:debug @state/global-state*)
    [:section.debug
     [:hr]
     [:h2 "Page Debug"]
     [:div
      [:h3 "@data*"]
      [:pre (with-out-str (pprint @data*))]]
     [:div
      [:h3 "@suspended-until*"]
      [:pre (with-out-str (pprint @suspended-until*))]]]))

;;; fetch ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def fetch-inventory-pool-user-suspension-id* (reagent/atom nil))
(defn fetch-inventory-pool-user-suspension []
  (let [resp-chan (async/chan)
        id (requests/send-off {:url (path :inventory-pool-user-suspension (-> @routing/state* :route-params))
                               :method :get
                               :query-params {}}
                              {:modal false
                               :title "Fetch Suspension"
                               :handler-key :inventory-pool-user-suspension
                               :retry-fn #'fetch-inventory-pool-user-suspension}
                              :chan resp-chan)]
    (reset! fetch-inventory-pool-user-suspension-id* id)
    (go (let [resp (<! resp-chan)]
          (when (and (#{200} (:status resp))
                     (= id @fetch-inventory-pool-user-suspension-id*))
            (reset! data* (:body resp)))))))


(defn clean-and-fetch []
  (reset! data* nil)
  (fetch-inventory-pool-user-suspension)
  (user/clean-and-fetch)
  (inventory-pool/clean-and-fetch))


;;; cancel ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn cancel [user & [cb & more]]
  (let  [user-suspension-path (path :inventory-pool-user-suspension
                                    {:user-id (:id user)
                                     :inventory-pool-id @inventory-pool-id*})]
    (let [resp-chan (async/chan)
          id (requests/send-off {:url user-suspension-path
                                 :method :delete }
                                {:modal true
                                 :title "Cancel suspension"}
                                :chan resp-chan)]
      (go (let [resp (<! resp-chan)]
            (when cb (cb resp)))))))

;;; delete ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def delete-inventory-pool-user-suspension-id* (reagent/atom nil))
(defn delete-inventory-pool-user-suspension []
  (let [resp-chan (async/chan)
        id (requests/send-off {:url (path :inventory-pool-user-suspension (-> @routing/state* :route-params))
                               :method :delete
                               :query-params {}}
                              {:modal true
                               :title "Delete Suspension"
                               :handler-key :inventory-pool-user-suspension
                               :retry-fn #'delete-inventory-pool-user-suspension}
                              :chan resp-chan)]
    (reset! delete-inventory-pool-user-suspension-id* id)
    (go (let [resp (<! resp-chan)]
          (when (and (#{204} (:status resp))
                     (= id @delete-inventory-pool-user-suspension-id*))
            (clean-and-fetch))))))

(defn remove-suspension-component []
  [:div
   (when (-> @data* :suspended_until)
     [:div.form.remove
      [:div.float-right
       [:button.btn.btn-warning
        {:on-click delete-inventory-pool-user-suspension}
        [:i.fas.fa-times]
      " suspend "  " cancel supension"]]
      [:div.clearfix]])])


;;; suspension component ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn put [_]
  (let [resp-chan (async/chan)
        id (requests/send-off {:url (path :inventory-pool-user-suspension {:inventory-pool-id @inventory-pool-id* :user-id @user-id*})
                               :method :put
                               :json-params  @data*}
                              {:modal true
                               :title "Update Suspensions"
                               :handler-key :inventory-pool-edit
                               :retry-fn #'put}
                              :chan resp-chan)]
    (go (let [resp (<! resp-chan)]
          (when (< (:status resp) 400)
            (clean-and-fetch))))))

(defn put-submit-component []
  [:div
   [:div.float-right
    [:button.btn.btn-warning
     {:on-click put}
     [:i.fas.fa-save]
     " Save "]]
   [:div.clearfix]])


(defn header-component []
  [:h1 "Suspension of "
   [user/user-name-component]
   " in "
   [inventory-pool/name-component]])

(defn humanized-suspended-until-component []
  [:div
   (if @suspended?*
     [:span.text-danger "This user is suspended for "
      (.to (:timestamp @state/global-state*) @suspended-until* true) "."]
     [:span.text-success "Not suspended."])])

(defn suspension-component []
  [:div.suspension
   [routing/hidden-state-component
    {:did-mount clean-and-fetch
     :did-change clean-and-fetch}]
   (if-not @data*
     [wait-component]
     [:div.form.edit
      [humanized-suspended-until-component]
      (when (or @edit-mode?* (-> @data* :suspended_until))
        [field-component [:suspended_until] data* edit-mode?* {:type :date}])
      (when  (or @edit-mode?* (-> @data* :suspended_reason))
        [field-component [:suspended_reason] data* edit-mode?* {:node-type :textarea}])
      (when @edit-mode?* [put-submit-component])])])

(defn page []
  [:div.inventory-pool-user-suspension
   [breadcrumbs/nav-component
    [(breadcrumbs/leihs-li)
     (breadcrumbs/admin-li)
     (breadcrumbs/inventory-pools-li)
     (breadcrumbs/inventory-pool-li @inventory-pool/inventory-pool-id*)
     (breadcrumbs/inventory-pool-users-li @inventory-pool/inventory-pool-id*)
     [breadcrumbs/inventory-pool-user-li @inventory-pool-id* @user-id*]
     [breadcrumbs/inventory-pool-user-suspension-li @inventory-pool-id* @user-id*]]
    []]
   [header-component]
   [:div.form
    [suspension-component]]
   [debug-component]])
