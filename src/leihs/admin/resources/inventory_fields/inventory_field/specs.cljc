(ns leihs.admin.resources.inventory-fields.inventory-field.specs
  (:require [clojure.spec.alpha :as s]
            [clojure.set :refer [union difference]]
            [clojure.string :refer [blank? lower-case]]
            [leihs.core.core :refer [presence?]]))

(def simple-types #{"date" "text" "textarea"})
(def advanced-types #{"checkbox" "radio" "select"})

(def new-dynamic-field-defaults
  "Default values used in frontend."
  {:active false,
   :data {:attribute ["properties"],
          :forPackage false,
          :group nil,
          :permissions {:role "lending_manager", :owner false},
          :type "text"}})

(def new-dynamic-field-constant-defaults
  "These can't be changed in frontend, but needs to be saved in DB."
  {:dynamic true, :position 0})

; ===================== ALLOWED KEYS ===========================

(def field-keys #{:active :data})
(def field-data-keys #{:label})
(def dynamic-field-data-keys #{:attribute
                               :label
                               :default
                               :forPackage
                               :group
                               :permissions
                               :target_type
                               :type
                               :values})
(def permissions-keys #{:role :owner})
(def values-keys #{:label :value})

; ==================== TOP-LEVEL KEYS ==========================

(s/def ::active boolean?)

(s/def :leihs.admin.field/data
  (s/merge (s/keys :req-un [::label])
           (s/map-of field-data-keys any?)))

(s/def :leihs.admin.dynamic-field/data
  (s/merge (s/keys :req-un [::label
                            ::group
                            ::permissions
                            ::type]
                   :opt-un [::forPackage ::target_type])
           (s/map-of dynamic-field-data-keys any?)
           ::values-according-to-type
           ::default-according-to-type))

(s/def :leihs.admin.new-dynamic-field/data
  (s/merge (s/keys :req-un [::attribute
                            ::forPackage
                            ::permissions
                            ::type]
                   :opt-un [::group
                            ::target_type])
           (s/map-of dynamic-field-data-keys any?)
           ::values-according-to-type
           ::default-according-to-type))

; ===================== DATA KEYS ==============================

(s/def ::attribute (s/tuple #{"properties"} presence?))
(s/def ::forPackage boolean?)
(s/def ::group (s/or :nil nil?
                     :string (s/and string?
                                    (complement blank?)
                                    #(-> % lower-case (not= "none")))))
(s/def ::default presence?)
(s/def ::default-according-to-type (fn [{ftype :type default :default :as data}]
                                     (if (#{"select" "radio"} ftype)
                                       (presence? default)
                                       (nil? default))))
(s/def ::is-active #(-> % :active true?))
(s/def ::label presence?)
(s/def ::one-of-values (s/merge (s/keys :req-un [::label ::value])
                                (s/map-of values-keys any?)))
(s/def ::owner boolean?)
(s/def ::permissions (s/merge (s/keys :req-un [::role ::owner])
                              (s/map-of permissions-keys any?)))
(s/def ::role #{"lending_manager" "inventory_manager"})
(s/def ::type (union simple-types advanced-types))
(s/def ::target_type (s/or :item-license #{"item" "license"} :nil nil?))
(s/def ::value presence?)
(s/def ::values (s/coll-of ::one-of-values :min-count 1))
(s/def ::values-according-to-type (fn [{ftype :type :as data}]
                                    (if (simple-types ftype)
                                      (not (contains? data :values))
                                      (s/valid? ::values (:values data)))))


; ===================== CORE/DYNAMIC FIELDS ====================

(s/def ::field
  (s/merge (s/keys :req-un [::active, :leihs.admin.field/data])
           (s/map-of field-keys any?)))

(s/def ::core-required-field (s/and ::field ::is-active))
(s/def ::core-not-required-field ::field)

(s/def ::dynamic-field
  (s/merge (s/keys :req-un [::active, :leihs.admin.dynamic-field/data])
           (s/map-of field-keys any?)))

(s/def ::dynamic-required-field (s/and ::field ::is-active))
(s/def ::dynamic-not-required-field ::dynamic-field)

(s/def ::new-dynamic-field
  (s/merge (s/keys :req-un [::active :leihs.admin.new-dynamic-field/data])
           (s/map-of field-keys any?)))

; ==============================================================

(comment
  (s/explain ::target_type "foo")
  (s/explain ::values-according-to-type {:type "text"})
  (s/explain ::attribute ["properties" ""])
  (s/explain :leihs.admin.new-dynamic-field/position 1)
  (s/explain (partial = #{1 2 3}) #{2 4 1 3})
  (s/explain ::dynamic-field
             {:foo "foo"
              :data {:label "label", :forPackage true}}))
