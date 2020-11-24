(ns golf.subs
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-sub
  :common/route
  (fn [db _]
    (-> db :common/route)))

(re-frame/reg-sub
  :common/page-id
  :<- [:common/route]
  (fn [route _]
    (-> route :data :name)))

(re-frame/reg-sub
  :common/page
  :<- [:common/route]
  (fn [route _]
    (-> route :data :view)))

;(re-frame/reg-sub
;  :docs
;  (fn [db _]
;    (:docs db)))

(re-frame/reg-sub
  :common/error
  (fn [db _]
    (:common/error db)))

(re-frame/reg-sub
  :user
  (fn [db _]
    (:user db)))

(re-frame/reg-sub
  :user/id
  :<- [:user]
  (fn [user _]
    (:id user)))

(re-frame/reg-sub
  :user/name
  :<- [:user]
  (fn [user _]
    (:name user)))

(re-frame/reg-sub
  :navbar-expanded?
  (fn [db _]
    (:navbar-expanded? db)))

(re-frame/reg-sub
  :game
  (fn [db _]
    (:game db)))

(re-frame/reg-sub
  :game/id
  :<- [:game]
  (fn [game _]
    (:id game)))

(re-frame/reg-sub
  :game/deck
  :<- [:game]
  (fn [game _]
    (:deck game)))

(re-frame/reg-sub
  :game/table-card
  :<- [:game]
  (fn [game _]
    (:table-card game)))

(re-frame/reg-sub
  :games
  (fn [db _]
    (:games db)))
