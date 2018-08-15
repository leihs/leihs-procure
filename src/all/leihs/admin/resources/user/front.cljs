(ns leihs.admin.resources.user.front
  (:refer-clojure :exclude [str keyword])
  (:require
    [leihs.admin.resources.user.front.create :as create]
    [leihs.admin.resources.user.front.edit :as edit]
    [leihs.admin.resources.user.front.inventory-pools-roles :as inventory-pools-roles]
    [leihs.admin.resources.user.front.remove :as remove]
    [leihs.admin.resources.user.front.shared :as user.shared]
    [leihs.admin.resources.user.front.show :as show]
    [leihs.admin.resources.user.front.password :as password]
    [leihs.admin.utils.core :refer [keyword str presence]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def delete-page remove/page)
(def new-page create/page)
(def edit-page edit/page)
(def show-page show/page)
(def password-page password/page)
(def inventory-pools-roles-page inventory-pools-roles/page)

