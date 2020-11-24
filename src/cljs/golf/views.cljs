(ns golf.views
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [golf.draw :as draw]
            [golf.websocket :as websocket]))

(defn nav-link [uri title page]
  [:a.navbar-item
   {:href   uri
    :class (when (= page @(re-frame/subscribe [:common/page])) :is-active)}
   title])

(defn navbar []
  (reagent/with-let [expanded? (re-frame/subscribe [:navbar-expanded?])]
    [:nav.navbar.is-info>div.container
     [:div.navbar-brand
      [:a.navbar-item {:href "/" :style {:font-weight :bold}} "golf"]
      [:span.navbar-burger.burger
       {:data-target :nav-menu
        :on-click #(re-frame/dispatch [:toggle-navbar-expanded])
        :class (when @expanded? :is-active)}
       [:span][:span][:span]]]
     [:div#nav-menu.navbar-menu
      {:class (when @expanded? :is-active)}
      [:div.navbar-start
       [nav-link "#/" "Home" :home]
       [nav-link "#/login" "Login" :login]
       [nav-link "#/games" "Games" :games]
       [nav-link "#/about" "About" :about]]]]))

(defn user-name-input [{:keys [value on-change]}]
  [:div.user-name-input
   [:input {:type :text
            :placeholder "Enter name"
            :value value
            :on-change on-change}]])

(defn login-button [{:keys [on-click]}]
  [:input {:type :button
           :value "Login"
           :on-click on-click}])

(defn login-form []
  (reagent/with-let
    [name (reagent/atom nil)]
    [:div.login-form
     [:h2 "Login"]
     [user-name-input {:value @name
                       :on-change #(reset! name (-> % .-target .-value))}]
     [login-button {:on-click #(if-not (nil? @name)
                                 (websocket/send-login! @name))}]]))

(defn info-display [name]
  [:div.info-display
   [:p "Logged in as " name]
   (if-let [game-id @(re-frame/subscribe [:game/id])]
     [:p "Connected to game " game-id])])

(defn logout-button [user-id]
  [:input.logout-button {:type "button"
                         :value "Logout"
                         :on-click #(do (websocket/send-logout! user-id)
                                        (re-frame/dispatch [:user/logout]))}])

(defn game-list []
  (let [games @(re-frame/subscribe [:games])]
    [:ul.game-list
     (for [game games]
       ^{:key (:id game)}
       [:li "Game " (:id game)])]))

(defn login-page []
  [login-form])

(defn about-page []
  [:img {:src "/img/warning_clojure.png"}])

(defn game-page []
  [:div.game-page
   [:h2 "Games"]
   [game-list]])

(defn home-page []
  [:div.home-page
   [:h2 "Let's play golf."]
   [draw/game-canvas]])

(defn page []
  (if-let [page @(re-frame/subscribe [:common/page])]
    [:div
     [navbar]
     [:section.section>div.container>div.content
      [page]
      (if-let [{:keys [id name]} @(re-frame/subscribe [:user])]
        [:div
         [info-display name]
         [logout-button id]])]]))
