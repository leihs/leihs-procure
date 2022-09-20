(ns leihs.admin.common.membership.users.shared
  (:require
    [leihs.admin.paths :as paths :refer [path]]
    #?@(:cljs
        [[reagent.ratom :as ratom :refer [reaction]]
         [leihs.core.routing.front :as routing]])
    [leihs.admin.resources.users.shared :as users-shared])
  )


(def DEFAULT-MEMBERSHIP-QUERY-PARAM "member")

(def MEMBERSHIP-QUERY-PARAM-KEY :membership)

(def QUERY-OPTIONS
  {"any" "members and non-members"
   "non" "non-members"
   DEFAULT-MEMBERSHIP-QUERY-PARAM "members"
   "direct" "direct members"
   "group" "group members"})

(def DEFAULT-QUERY-PARAMS
  (merge users-shared/default-query-params
         {:membership "member"}))




;;; helpers ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(def filtered-by-member?*
  #?(:cljs
     (reaction
       (= "member" (get-in @routing/state* [:query-params-raw :membership] DEFAULT-MEMBERSHIP-QUERY-PARAM)))))

(defn empty-members-alert []
  #?(:cljs
     [:div.alert.alert-warning.text-center
      [:p "No users found but the current filter seetings seem to include only members."]
      [:p [:a.btn.btn-outline-primary
           {:href (path (:path @routing/state*) {}
                        (assoc (:query-params-raw @routing/state*) :membership "any"))}
           "Show members and non-members"]]]))

