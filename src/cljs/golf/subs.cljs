(ns golf.subs
  (:require [re-frame.core :as re-frame]
            [golf.game :as game]))

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
  :game/started?
  :<- [:game]
  (fn [game _]
    (:started? game)))

(re-frame/reg-sub
  :game/next-player
  :<- [:game]
  (fn [game _]
    (:id (game/next-player game))))

(re-frame/reg-sub
  :games
  (fn [db _]
    (:games db)))

(re-frame/reg-sub
  :players
  (fn [db _]
    (vals (get-in db [:game :players]))))

(re-frame/reg-sub
  :player/turn
  :<- [:user/id]
  :<- [:game]
  (fn [[user-id game] _]
    (get-in game [:players user-id :turn])))

(re-frame/reg-sub
  :clicked-card
  (fn [db _]
    (:clicked-card db)))
