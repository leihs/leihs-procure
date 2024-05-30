(ns leihs.admin.resources.users.user.api-tokens.core
  (:require ["date-fns" :as date-fns]
            [cljs.core.async :as async :refer [<! go]]
            [clojure.string :refer [join split capitalize]]
            [leihs.admin.common.http-client.core :as http-client]
            [leihs.admin.utils.misc :as front-shared :refer [humanize-datetime-component]]
            [leihs.core.core :refer [presence str]]
            [leihs.core.paths :refer [path]]
            [leihs.core.routing.front :as routing]
            [reagent.core :as reagent]))

(defn humanize-scope [scope]
  (->> (-> scope str (split "_"))
       (drop 1)
       (map capitalize)
       (join " ")))

(defn summarize-scopes [{:keys [scope_read scope_write scope_admin_read scope_admin_write scope_system_admin_read scope_system_admin_write]}]
  (let [get-rw-text (fn [r w] (cond (and r w) "Read+Write" r "Read" w "Write"))]
    (->> {"" (get-rw-text scope_read scope_write)
          "Admin" (get-rw-text scope_admin_read scope_admin_write)
          "System Admin" (get-rw-text scope_system_admin_read scope_system_admin_write)}
         (filter (fn [[_ v]] (some? v)))
         (map (fn [[k v]] (join " " [k v])))
         (join ", "))))

(def scopes [:scope_read :scope_write :scope_admin_read :scope_admin_write :scope_system_admin_read :scope_system_admin_write])

(defonce data* (reagent/atom nil))

(defn reset-to-defaults []
  (reset! data* {:description nil
                 :scope_read true
                 :scope_write true
                 :scope_admin_read true
                 :scope_admin_write true
                 :expires_at (date-fns/formatISO
                              (date-fns/add (js/Date.)
                                            (clj->js {:years 1})))}))

(defn clean-and-fetch []
  (reset! data* nil)
  (go (reset! data*
              (some->
               {:url (path :api-token (-> @routing/state* :route-params))
                :chan (async/chan)}
               http-client/request :chan <!
               http-client/filter-success! :body))))

(defn on-change-datetime-local [e]
  (let [value (-> e .-target .-value)
        iso (-> value date-fns/parseISO date-fns/formatISO)]
    (swap! data* assoc :expires_at iso)))

(defn scope-disabled? [scope]
  (case scope
    :scope_read (or (-> @data* :scope_admin_read)
                    (-> @data* :scope_system_admin_read)
                    (-> @data* :scope_write))
    :scope_write (or (-> @data* :scope_read not)
                     (-> @data* :scope_admin_write)
                     (-> @data* :scope_system_admin_write))
    :scope_admin_read (or (-> @data* :scope_read not)
                          (-> @data* :scope_admin_write))
    :scope_admin_write (or (-> @data* :scope_admin_read not)
                           (-> @data* :scope_write not))
    :scope_system_admin_read (or (-> @data* :scope_read not)
                                 (-> @data* :scope_system_admin_write))
    :scope_system_admin_write (or (-> @data* :scope_system_admin_read not)
                                  (-> @data* :scope_write not))))

(defn scope-form-component [scope]
  [:div.checkbox
   {:key (str scope)}
   [:label
    [:input {:id (str scope)
             :type :checkbox
             :disabled (scope-disabled? scope)
             :checked (-> @data* scope boolean)
             :on-change #(swap! data* assoc scope (-> @data* scope presence not))}]
    [:span
     {:class (if (scope-disabled? scope) "text-muted" "")}
     " "
     (humanize-scope scope)]]])

(defn expires-presets-component []
  [:small.form-text "Presets: "
   (doall
    (for [[period v] {"day" {:days 1}
                      "week" {:weeks 1}
                      "month" {:months 1}
                      "year" {:years 1}
                      "never" {:years 1000}}]
      [:span {:key period} " "
       [:a.btn.btn-sm.btn-outline-secondary
        {:id period
         :href "#"
         :on-click #(swap! data* assoc
                           :expires_at
                           (date-fns/formatISO
                            (date-fns/add (js/Date.) (clj->js v))))}
        period]]))])

(defn form [& {:keys [on-save]}]
  [:form#api-token-form.form
   {:on-submit (fn [e]
                 (.preventDefault e)
                 (on-save))}

   [:div.form-group
    [:label {:for :description} [:b "Description:"]]
    [:input#description.form-control
     {:class ""
      :on-change #(swap! data* assoc :description (-> % .-target .-value presence))
      :value (-> @data* :description)
      :required true}]]

   [:div.form-group
    [:label [:b "Scope and actions:"]]
    (doall (for [scope scopes] ^{:key scope} [scope-form-component scope]))
    (when true
      [:small.form-text
       {:class (when-not (-> @data* :scope_read presence boolean) "text-warning")}
       "Not setting at least \"read\" will practically disable this token!"]
      [:small.form-text
       "Read and write correspond to perform actions
         via safe (read) or unsafe (write) HTTP verbs."]
      [:small.form-text
       "Enabled admin scopes will have effect if and only if the corresponding
         user has admin privileges at the time this tokes is used."])]

   [:div.form-group {:class ""}
    [:label {:for :expires_at} [:b "Expires" ":"]]
    [:input#expires_at.form-control
     {:class ""
      :type :datetime-local
      :on-change on-change-datetime-local
      :value (some-> @data* :expires_at date-fns/parseISO
                     (date-fns/format "yyyy-MM-dd'T'HH:mm"))}]
    [expires-presets-component]]

   (when-let [created-at (-> @data* :created_at)]
     [:div.form-group
      [:p.form-text
       "This token has been created " (humanize-datetime-component created-at)
       (when-let [updated-at (-> @data* :updated_at)]
         [:<>
          ", and updated "
          (humanize-datetime-component updated-at)])
       ". "]])])
