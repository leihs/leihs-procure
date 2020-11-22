(ns leihs.admin.resources.inventory-pools.inventory-pool.users.user.suspension.main
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
     [leihs.core.core :refer [keyword str presence]]
     [leihs.core.requests.core :as requests]
     [leihs.core.routing.front :as routing]
     [leihs.core.icons :as icons]

     [leihs.admin.common.components :as components]
     [leihs.admin.common.form-components :as form-components]
     [leihs.admin.utils.misc :refer [wait-component]]
     [leihs.admin.state :as state]
     [leihs.admin.paths :as paths :refer [path]]
     [leihs.admin.resources.inventory-pools.inventory-pool.core :as inventory-pool]
     [leihs.admin.resources.inventory-pools.inventory-pool.users.user.breadcrumbs :as breadcrumbs]
     [leihs.admin.resources.users.user.core :as user :refer [user-id* user-data*]]
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
  (fetch-inventory-pool-user-suspension))


;;; cancel ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn cancel [user & [cb & more]]
  (let  [user-suspension-path (path :inventory-pool-user-suspension
                                    {:user-id (:id user)
                                     :inventory-pool-id @inventory-pool/id*})]
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

(defn put [& args]
  (let [resp-chan (async/chan)
        id (requests/send-off {:url (path :inventory-pool-user-suspension {:inventory-pool-id @inventory-pool/id* :user-id @user-id*})
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


(defn header-component []
  [:h1 "Suspension of "
   [user/name-link-component]
   " in "
   [inventory-pool/name-link-component]])

(defn humanized-suspended-until-component []
  [:div
   (if @suspended?*
     [:span.text-danger "This user is suspended for "
      (.to (:timestamp @state/global-state*) @suspended-until* true) "."]
     [:span.text-success "Not suspended."])])

(defn suspension-component []
  [:div.suspension
   [routing/hidden-state-component
    {:did-change clean-and-fetch}]
   (if-not @data*
     [wait-component]
     [:div
      [humanized-suspended-until-component]
      [:form.form.mt-3
       {:on-submit (fn [e] (.preventDefault e) (put))}
       [form-components/input-component data* [:suspended_until]
        :type :date
        :label "Suspended until"]
       [form-components/input-component data* [:suspended_reason]
        :element :textarea
        :rows 5
        :label "Reason"]
       [form-components/save-submit-component]]])])

(defn page []
  [:div.inventory-pool-user-suspension
   [breadcrumbs/nav-component
    (conj @breadcrumbs/left* [breadcrumbs/suspension-li])[]]
   [header-component]
   [:div.form
    [suspension-component]]
   [debug-component]])
