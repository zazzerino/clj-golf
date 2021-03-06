(ns golf.core
  (:require
   [day8.re-frame.http-fx]
   [reagent.core :as r]
   [reagent.dom :as dom]
   [re-frame.core :as rf]
   [markdown.core :refer [md->html]]
   [mount.core :as mount]
   [golf.ajax :as ajax]
   [golf.draw]
   [golf.db]
   [golf.events]
   [golf.subs]
   [golf.views :as views]
   [golf.websocket :as ws]
   [reitit.core :as reitit]
   [reitit.frontend.easy :as rfe])
  (:import goog.History))

(defn navigate! [match _]
  (rf/dispatch [:common/navigate match]))

(def router
  (reitit/router
    [["/" {:name :home
           :view #'views/home-page}]
     ["/login" {:name :login
                :view #'views/login-page}]
     ["/games" {:name :games
                :view #'views/game-page
                :controllers [{:start #(ws/send-get-games!)}]}]
     ["/about" {:name :about
                :view #'views/about-page}]]))

(defn start-router! []
  (rfe/start!
   router
   navigate!
   {}))

;; -------------------------
;; Initialize app
(defn ^:dev/after-load mount-components []
  (rf/clear-subscription-cache!)
  (dom/render [#'views/page] (.getElementById js/document "app")))

(set! js/window.onclick #(if @(rf/subscribe [:navbar-expanded?])
                           (rf/dispatch [:toggle-navbar-expanded])))

(defn init! []
  (start-router!)
  (ajax/load-interceptors!)
  (rf/dispatch-sync [:initialize-db])
  (mount/start)
  (mount-components))
