(ns golf.subs
  (:require [re-frame.core :as rf]))

(rf/reg-sub
  :common/route
  (fn [db _]
    (-> db :common/route)))

(rf/reg-sub
  :common/page-id
  :<- [:common/route]
  (fn [route _]
    (-> route :data :name)))

(rf/reg-sub
  :common/page
  :<- [:common/route]
  (fn [route _]
    (-> route :data :view)))

;(re-frame/reg-sub
;  :docs
;  (fn [db _]
;    (:docs db)))

(rf/reg-sub
  :common/error
  (fn [db _]
    (:common/error db)))

(rf/reg-sub
  :user
  (fn [db _]
    (:user db)))

(rf/reg-sub
  :user/id
  :<- [:user]
  (fn [user _]
    (:id user)))

(rf/reg-sub
  :user/name
  :<- [:user]
  (fn [user _]
    (:name user)))

(rf/reg-sub
  :navbar-expanded?
  (fn [db _]
    (:navbar-expanded? db)))

(rf/reg-sub
  :game
  (fn [db _]
    (:game db)))

(rf/reg-sub
  :game/id
  :<- [:game]
  (fn [game _]
    (:id game)))

(rf/reg-sub
  :game/deck
  :<- [:game]
  (fn [game _]
    (:deck game)))

(rf/reg-sub
  :game/table-card
  :<- [:game]
  (fn [game _]
    (:table-card game)))

(rf/reg-sub
  :games
  (fn [db _]
    (:games db)))

(rf/reg-sub
  :players
  (fn [db _]
    (vals (get-in db [:game :players]))))
