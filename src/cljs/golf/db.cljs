(ns golf.db
  (:require [cljs.spec.alpha :as spec]))

(spec/def :user/id string?)
(spec/def :user/name string?)
(spec/def ::user (spec/nilable (spec/keys :req-un [:user/id :user/name])))

(spec/def ::db (spec/keys :req-un [::user]))

(def default-db
  {:user nil})
