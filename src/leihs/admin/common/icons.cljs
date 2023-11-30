(ns leihs.admin.common.icons
  (:refer-clojure :exclude [next])
  (:require
   ["@fortawesome/free-brands-svg-icons" :as brands]
   ["@fortawesome/free-solid-svg-icons" :as solids]
   ["@fortawesome/react-fontawesome" :as fa-react-fontawesome :refer [FontAwesomeIcon]]))

(defn add [] (FontAwesomeIcon #js{:icon solids/faPlusCircle :className ""}))
(defn admin [] (FontAwesomeIcon #js{:icon solids/faWrench :className ""}))
(defn authentication-system [] (FontAwesomeIcon #js{:icon solids/faExternalLinkAlt :className ""}))
(defn authentication-systems [] (FontAwesomeIcon #js{:icon solids/faExternalLinkAlt :className ""}))
(defn building [] (FontAwesomeIcon #js{:icon solids/faBuilding :className ""}))
(defn delegation [] (FontAwesomeIcon #js{:icon solids/faHandsHelping :className ""}))
(defn delegations [] (FontAwesomeIcon #js{:icon solids/faHandsHelping :className ""}))
(defn delete [] (FontAwesomeIcon #js{:icon solids/faTimes :className ""}))
(defn edit [] (FontAwesomeIcon #js{:icon solids/faEdit :className ""}))
(defn email [] (FontAwesomeIcon #js{:icon solids/faEnvelope :className ""}))
(defn entitlement-groups [] (FontAwesomeIcon #js{:icon solids/faObjectGroup :className ""}))
(defn github [] (FontAwesomeIcon #js{:icon brands/faGithubSquare :className ""}))
(defn group [] (FontAwesomeIcon #js{:icon solids/faUsers :className ""}))
(defn groups [] (FontAwesomeIcon #js{:icon solids/faUsers :className ""}))
(defn home [] (FontAwesomeIcon #js{:icon solids/faHome :className ""}))
(defn inventory [] (FontAwesomeIcon #js{:icon solids/faCube :className ""}))
(defn inventory-field [] (FontAwesomeIcon #js{:icon solids/faCode :className ""}))
(defn inventory-fields [] (FontAwesomeIcon #js{:icon solids/faCode :className ""}))
(defn inventory-pool [] (FontAwesomeIcon #js{:icon solids/faCube :className ""}))
(defn inventory-pools [] (FontAwesomeIcon #js{:icon solids/faCubes :className ""}))
(defn mail-template [] (FontAwesomeIcon #js{:icon solids/faEnvelope :className ""}))
(defn mail-templates [] (FontAwesomeIcon #js{:icon solids/faEnvelope :className ""}))
(defn password-reset [] (FontAwesomeIcon #js{:icon solids/faKey :className ""}))
(defn rooms [] (FontAwesomeIcon #js{:icon solids/faWarehouse :className ""}))
(defn save [] (FontAwesomeIcon #js{:icon solids/faSave :className ""}))
(defn suppliers [] (FontAwesomeIcon #js{:icon solids/faTruck :className ""}))
(defn system [] (FontAwesomeIcon #js{:icon solids/faUserServer :className ""}))
(defn system-admin [] (FontAwesomeIcon #js{:icon solids/faUserAstronaut :className ""}))
(defn system-admins [] (FontAwesomeIcon #js{:icon solids/faUserAstronaut :className ""}))
(defn user [] (FontAwesomeIcon #js{:icon solids/faUser :className ""}))
(defn users [] (FontAwesomeIcon #js{:icon solids/faUserFriends :className ""}))
(defn view [] (FontAwesomeIcon #js{:icon solids/faEye :className ""}))
(defn waiting [& {:keys [size] :or {size "1x"}}] (FontAwesomeIcon #js{:icon solids/faCircleNotch :className "" :spin true :size size}))



