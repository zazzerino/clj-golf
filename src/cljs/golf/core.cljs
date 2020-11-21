(ns golf.core
  (:require
   [day8.re-frame.http-fx]
   [reagent.dom :as dom]
   [re-frame.core :as re-frame]
   [markdown.core :refer [md->html]]
   [golf.ajax :as ajax]
   [golf.events]
   [golf.subs]
   [golf.views :as views]
   [golf.websocket :as ws]
   [reitit.core :as reitit]
   [reitit.frontend.easy :as rfe])
  (:import goog.History))

(defn navigate! [match _]
  (re-frame/dispatch [:common/navigate match]))

(def router
  (reitit/router
    [["/" {:name :home
           :view #'views/home-page
           ;:controllers [{:start (fn [_] (rf/dispatch [:page/init-home]))}]
           }]
     ["/login" {:name :login
                :view #'views/login-page}]
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
  (re-frame/clear-subscription-cache!)
  (dom/render [#'views/page] (.getElementById js/document "app")))

;(set! js/window.onclick #(if @(re-frame/subscribe [:navbar-expanded?])
;                           (re-frame/dispatch [:toggle-navbar-expanded])))

(defn init! []
  (start-router!)
  (ajax/load-interceptors!)
  (re-frame/dispatch-sync [:initialize-db])
  (ws/make-websocket! (str "ws://" (.-host js/location) "/ws")
                      ws/handle-response)
  (mount-components))
