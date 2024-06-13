(ns leihs.admin.common.components.navigation.breadcrumbs
  (:require
   [cljs.core.async :as async :refer [<! go]]
   [clojure.string :as string]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.common.icons :as icons]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.core.routing.front :as routing]
   [leihs.core.user.front :as current-user]
   [react-bootstrap :refer [Breadcrumb BreadcrumbItem]]
   [reagent.core :as reagent]))

(defonce history* (reagent/atom []))

;; ##############################################
;; Configure Breadcrumb Generation
;; ##############################################

;; Define the handlers that should be ignored
;; when adding a new entry to the history

(def ignore-handler [:statistics
                     :inventory
                     :users-choose
                     :user-choose
                     :home])

;; Define the handlers that should trigger a reset
;; Those are basically all handler that correspond 
;; to a navigation entry in the sidebar

(def reset-handler [:inventory-pools
                    :groups
                    :users
                    :inventory-fields
                    :buildings
                    :rooms
                    :suppliers
                    :mail-templates
                    :authentication-systems])

;; Define the resolvers for the breadcrumb names
;; The keys are the resolvers and the values are
;; vectors of regular expressions that should match
;; the handler key
;;
;; CAUTION: The order of the vectors and the keys is important

(def resolvers
  {:inventory-pool [":inventory-pool-delegations"
                    ":inventory-pool-groups"
                    ":inventory-pool-opening-times"
                    ":inventory-pool-entitlement-groups"
                    ":inventory-pool-users"]
   :inventory-pool-delegation [":inventory-pool-delegation.*"]
   :inventory-pool-entitlement-group [":inventory-pool-entitlement-group-.*"]
   :inventory-pool-user [":inventory-pool-user"]
   :authentication-system [":authentication-system.*"]
   :user [":user.*"]
   :group [":group.*"]})

(defn- find-last-index [data key item]
  (->> data
       (map-indexed #(when (= (key %2) (key item)) %1))
       (keep-indexed #(when %2 %1))
       last))

(defn- capitalize-and-replace
  "This function takes a symbol, converts it to a string, and modifies it.

  If the string contains a hyphen, it splits the string at the hyphen, capitalizes each part, 
  and then joins the parts together with a space. If the string does not contain a hyphen, 
  it simply capitalizes the string.

  Parameters:
  - `symbol`: The symbol to be converted and modified. It's expected to be a Clojure symbol.

  Returns:
  - A string that is the modified version of the input symbol."

  [symbol]
  (as-> symbol s
    (name s)
    (if (clojure.string/includes? s "-")
      (as-> s st
        (string/split st #"-")
        (map string/capitalize st)
        (string/join " " st))
      (string/capitalize s))))

(defn- update-history
  "This function updates the history atom with the next navigation state.

  It first checks if the next navigation state already exists in the history. 
  If it does and it's the last entry, the function replaces the last entry with the next state. 
  Otherwise, it simply adds the next state to the end of the history.

  Parameters:
  - `next`: The next navigation state. It's a map that contains the name, URL, resolver, and handler of the next state.

  Returns:
  - Nothing. The function updates the history atom in place."

  [next]
  (let [existing-index (find-last-index @history* :resolver next)]
    (if (= existing-index (dec (count @history*)))
      (reset! history* (conj (subvec @history* 0 existing-index) next))
      (swap! history* conj next))))

(defn- user? [resolver]
  (or (= resolver :inventory-pool-user)
      (= resolver :user)))

(defn- get-name [next]
  (when (:resolver next)
    (go
      (let [resolver (:resolver next)
            res  (->
                  {:chan (async/chan)
                   :url (path resolver
                              (-> @routing/state* :route-params))}
                  http-client/request :chan <!
                  http-client/filter-success! :body)]
        (if (user? resolver)
          (str (:firstname res) " " (:lastname res))
          (str (:name res)))))))

(def popstate? (atom false))

;; Define a function to handle the popstate event
(defn- handle-popstate []
  (reset! popstate? true))

(defn- addable? []
  (and @current-user/state*
       (not (= (:handler-key @routing/state*) :admin))
       (not (some #(= (:handler-key @routing/state*) %) ignore-handler))))

(defn- reset? []
  (some #(= (:handler-key @routing/state*) %) reset-handler))

(defn- get-resolver
  "This function takes two arguments: `handler` and `resolver`.
   `handler` is expected to be a string, and `resolvers` is expected to be a map.
   
   The function iterates over the `resolvers` map. For each key-value pair in the map:
   - It checks if the `handler` string matches the regular expression pattern created from each string in the vector `matchers`.
   - If a match is found, it returns the corresponding key `resolver`.
   - If no match is found after checking all vectors, it returns nil.
   
   This function can be used to find the key in a map that corresponds to a string matching a certain pattern."

  [handler resolvers]
  (reduce (fn [acc [resolver matcher]]
            (or acc
                (some #(when (re-matches (re-pattern %) handler) resolver) matcher)))
          nil
          resolvers))

(defn- next-entry
  "This function checks if the current hanlder-key of the route is 
  matching agains one of the matchers in the resolver-keys map.

  Those resolver-keys are used to resolve the name of the breadcrumb.

  Parameters:
  - `state`: this should be the current routing state from @routing/state*

  Returns:
  - A map containing the name, URL, resolver, and handler of the next state."
  [state]
  (let [new-entry {:url (:route state)
                   :path (:path state)
                   :handler (:handler-key state)}
        handler (:handler-key state)
        resolver (get-resolver (str handler) resolvers)]

    (if (and resolver (not (reset?)))
      (conj {:resolver resolver} new-entry)

      (if (reset?)
        (conj {:name (capitalize-and-replace handler)
               :resolver nil}
              new-entry)
        (conj {:resolver handler})))))

(defn watcher
  "This function creates a Reagent component that manages the history of navigation events.

  The component has several lifecycle methods:

  - `:component-did-mount`: Adds an event listener for 'popstate' events on the window object. 
    This event is fired when the active history entry changes due to a user action.

  - `:component-will-unmount`: Removes the 'popstate' event listener from the window object.

  - `:component-did-update`: Checks if a 'popstate' event has occurred and if the history has more than one entry. 
    If the URL of the second last entry in the history matches the URL of the next routing state, 
    it removes the last entry from the history. It then resets the 'popstate' atom to false.

  - `:reagent-render`: Checks if a new entry can be added to the history. If a reset is needed, 
    it resets the 'popstate' atom to false and the history to contain only the next routing state. 
    Otherwise, it updates the history with the next routing state.

  The component returns a React fragment."

  []
  (reagent/create-class
   {:component-did-mount
    (fn []
      (.. js/window (addEventListener "popstate" handle-popstate)))

    :component-will-unmount
    (fn []
      (.. js/window (removeEventListener "popstate" handle-popstate)))

    :component-did-update
    (fn []
      (let [history @history*
            next (next-entry @routing/state*)]
        (when (and @popstate?
                   (> (count history) 1))
          (when (= (:url (nth history (- (count history) 2))) (:url next))
            (reset! history* (subvec @history* 0 (dec (count @history*)))))
          (reset! popstate? false))))

    :reagent-render
    (fn []
      (when (addable?)
        (let [next (next-entry @routing/state*)]
          (if (reset?)
            (do
              (reset! popstate? false)
              (reset! history* [next]))
            (go
              (update-history {:name (<! (get-name next))
                               :url (:url next)
                               :resolver (:resolver next)
                               :handler (:handler next)}))))

        [:<>]))}))

(defn main
  "This function creates a Reagent component that renders 
  the navigation history as breadcrumbs."
  []
  (when (> (count @history*) 1)
    [:<>
     [:span {:class-name "text-muted"} "Navigation History"]
     [:> Breadcrumb
      {:style {:max-width "fit-content"}}
      [:span {:class-name "mr-2" :style {:color "#007bff"}}
       [icons/clock-rotate-left]
       [:span {:class-name "mr-2"} ""]]
      (doall (map-indexed (fn [index breadcrumb]
                            [:> BreadcrumbItem
                             {:style {:max-width (if (> index (- (count @history*) 3))
                                                   "fit-content"
                                                   (str (if (< (count @history*) 6) 150 50) "px"))
                                      :text-overflow "ellipsis"
                                      :white-space "nowrap"
                                      :overflow "hidden"}
                              :href (str (:url breadcrumb))
                              :data-test-id (str (:name breadcrumb))
                              :key index
                              :title (:name breadcrumb)
                              :active (when (= (inc index) (count @history*)) true)}
                             (if-let [icon (:icon breadcrumb)]
                               [icon]
                               [:span {:on-click #(swap! history* (fn [data] (subvec data 0 (inc index))))}
                                (str (:name breadcrumb))])])
                          @history*))]]))
