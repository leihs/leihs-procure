(ns leihs.admin.resources.inventory-pools.inventory-pool.holidays.edit
  (:require
   [accountant.core :as accountant]
   [cljs.core.async :as async :refer [go <!]]
   [clojure.string :refer [capitalize]]
   [com.rpl.specter :as s]
   [leihs.admin.common.components.table :as table]
   [leihs.admin.common.form-components :as form-components]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.inventory-pools.authorization :as pool-auth]
   [leihs.admin.resources.inventory-pools.inventory-pool.core :as pool-core]
   [leihs.admin.resources.inventory-pools.inventory-pool.holidays.core :as core]
   [leihs.admin.utils.misc :refer [wait-component]]
   [leihs.core.auth.core :as auth]
   [leihs.core.constants :as constants]
   [leihs.core.core :refer [presence]]
   [leihs.core.front.debug :refer [spy]]
   [react-bootstrap :as BS :refer [Button Form Modal]]
   [reagent.core :as reagent]))

(defonce data* (reagent/atom nil))
(comment (deref data*)
         @pool-core/data*)

(defn prepare-for-patch [data]
  (->> data
       (map #(if (:new %) (dissoc % :new :id) %))))

(defn patch []
  (let [route (path :inventory-pool-holidays
                    {:inventory-pool-id (:id @pool-core/data*)})]
    (go (when (some->
               {:url route
                :method :patch
                :json-params (prepare-for-patch @data*)
                :chan (async/chan)}
               http-client/request :chan <!
               http-client/filter-success!)
          (reset! data* nil)
          (core/clean-and-fetch)))))

(defn add-new-holiday-comp []
  (let [new-holiday (reagent/atom {:inventory_pool_id (:id @pool-core/data*)})
        end-date-before-start-date? (reagent/atom false)]
    (fn []
      (let [today-min (-> (new js/Date) .toISOString (.split "T") first)
            validate-dates! #(reset! end-date-before-start-date?
                                     (and (:end_date @new-holiday)
                                          (:start_date @new-holiday)
                                          (< (js/Date. (:end_date @new-holiday))
                                             (js/Date. (:start_date @new-holiday)))))]
        [:<>
         (when @end-date-before-start-date?
           [:div.alert.alert-danger "End date cannot be before start date."])
         [:form.form-inline {:on-submit (fn [e]
                                          (.preventDefault e)
                                          (swap! data* conj
                                                 (assoc @new-holiday
                                                        :new true
                                                        :id (str (random-uuid))))
                                          (reset! new-holiday nil))}
          [:div.form-group.mb-2.mr-2.w-50
           [:input.form-control.w-100 {:type "text" :placeholder "Name"
                                       :value (:name @new-holiday)
                                       :required true
                                       :on-change #(swap! new-holiday
                                                          assoc :name
                                                          (-> % .-target .-value))}]]
          [:div.form-group.mb-2.mr-2
           [:input.form-control {:type "date" ;:placeholder "From"
                                 :id "start-date"
                                 :value (:start_date @new-holiday)
                                 :min today-min
                                 :required true
                                 :on-change #(do (swap! new-holiday
                                                        assoc :start_date
                                                        (-> % .-target .-value))
                                                 (validate-dates!))}]]
          [:div.form-group.mb-2.mr-2
           [:input.form-control {:type "date" ;:placeholder "To"
                                 :id "end-date"
                                 :value (:end_date @new-holiday)
                                 :min today-min
                                 :required true
                                 :on-change #(do (swap! new-holiday
                                                        assoc :end_date
                                                        (-> % .-target .-value))
                                                 (validate-dates!))}]]
          [:> Button {:type "submit"
                      :className "btn-info mb-2"
                      :disabled @end-date-before-start-date?}
           "Add"]]]))))

(defn holiday-row-comp [holiday]
  [:tr {:key (:id holiday)
        :class (when (:delete holiday) "table-danger")}
   [:td (cond->> (:name holiday) (:delete holiday) (vector :s))]
   [:td (cond->> (:start_date holiday) (:delete holiday) (vector :s))]
   [:td (cond->> (:end_date holiday) (:delete holiday) (vector :s))]
   [:td
    (let [specter-path [s/ALL #(= (:id %) (:id holiday))]]
      (if (:delete holiday)
        [:> Button
         {:onClick
          (fn [_]
            (swap! data*
                   (fn [d]
                     (s/transform specter-path #(dissoc % :delete) d))))
          :variant "outline-secondary" :size "sm"}
         "Restore"]
        [:> Button
         {:onClick
          (fn [_]
            (swap! data*
                   (fn [d]
                     (if (:new holiday)
                       (s/setval specter-path s/NONE d)
                       (s/transform specter-path #(assoc % :delete true) d)))))
          :variant "outline-danger" :size "sm"}
         "Delete"]))]])

(defn form [on-hide]
  (let [new-holiday (reagent/atom nil)]
    (fn [on-hide]
      (if-not @data*
        [wait-component]
        [:<>
         [add-new-holiday-comp]
         [:> Form {:id "workdays-form"
                   :on-submit (fn [e] (.preventDefault e) (on-hide) (patch))}
          [table/container
           {:borders false
            :header [:tr [:th "Day"] [:th "From"] [:th "To"] [:th]]
            :body
            (doall (for [holiday @data*]
                     [holiday-row-comp holiday]))}]]]))))

(comment (let [hs [{:id 1} {:id 2}]]
           (s/transform [s/ALL #(= (:id %) 1)]
                        #(assoc % :delete true)
                        hs)))

(defn dialog [& {:keys [show onHide] :or {show false}}]
  [:> Modal {:size "lg"
             :scrollable true
             :centered true
             :show show}
   [:> Modal.Header {:closeButton true
                     :onHide onHide}
    [:> Modal.Title "Edit Holidays"]]
   [:> Modal.Body [form onHide]]
   [:> Modal.Footer
    [:> Button {:variant "secondary" :onClick onHide}
     "Cancel"]
    [:> Button {:type "submit" :form "workdays-form"}
     "Save"]]])

(defn button []
  (when (auth/allowed? [pool-auth/pool-inventory-manager?
                        auth/admin-scopes?])
    (let [show (reagent/atom false)]
      (fn []
        [:<>
         [:> Button
          {:onClick #(do (when-not @data* (reset! data* @core/data*))
                         (reset! show true))}
          "Edit"]
         [dialog {:show @show
                  :onHide #(reset! show false)}]]))))
