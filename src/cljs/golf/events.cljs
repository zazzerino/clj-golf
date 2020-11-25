(ns golf.events
  (:require
    [re-frame.core :as rf]
    ;[ajax.core :as ajax]
    [reitit.frontend.easy :as rfe]
    [reitit.frontend.controllers :as rfc]
    [golf.db :as db]))

(rf/reg-event-db
 :initialize-db
 (fn [_ _]
   db/default-db))

(rf/reg-fx
  :common/navigate-fx!
  (fn [[k & [params query]]]
    (rfe/push-state k params query)))

(rf/reg-event-db
  :common/navigate
  (fn [db [_ match]]
    (let [old-match (:common/route db)
          new-match (assoc match :controllers
                           (rfc/apply-controllers (:controllers old-match)
                                                  match))]
      (assoc db :common/route new-match))))

(rf/reg-event-fx
  :common/navigate!
  (fn [_ [_ url-key params query]]
    {:common/navigate-fx! [url-key params query]}))

;(re-frame/reg-event-db
;  :set-docs
;  (fn [db [_ docs]]
;    (assoc db :docs docs)))

;(re-frame/reg-event-fx
;  :fetch-docs
;  (fn [_ _]
;    {:http-xhrio {:method          :get
;                  :uri             "/docs"
;                  :response-format (ajax/raw-response-format)
;                  :on-success      [:set-docs]}}))

(rf/reg-event-db
  :common/set-error
  (fn [db [_ error]]
    (assoc db :common/error error)))

;(re-frame/reg-event-fx
;  :page/init-home
;  (fn [_ _]
;    {:dispatch [:fetch-docs]}))

(rf/reg-event-db
  :user/login
  (fn [db [_ user]]
    (assoc db :user user)))

(rf/reg-event-db
  :user/logout
  (fn [db _]
    (assoc db :user nil :game nil)))

(rf/reg-event-db
  :toggle-navbar-expanded
  (fn [db _]
    (update-in db [:navbar-expanded?] not)))

(rf/reg-event-db
  :set-game
  (fn [db [_ game]]
    (assoc db :game game)))

(rf/reg-event-db
  :set-games
  (fn [db [_ games]]
    (assoc db :games games)))
