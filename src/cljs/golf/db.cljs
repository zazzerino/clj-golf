(ns golf.db
  (:require [cljs.spec.alpha :as spec]))

(spec/def :user/id string?)
(spec/def :user/name string?)
(spec/def ::user (spec/nilable (spec/keys :req-un [:user/id :user/name])))

(spec/def ::navbar-expanded? boolean?)

;(spec/def ::db (spec/keys :req-un [::user ::navbar-expanded?]))

(def default-db
  {:navbar-expanded? false
   :user nil
   :game nil
   :games nil})
