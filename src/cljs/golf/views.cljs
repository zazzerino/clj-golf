(ns golf.views
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [golf.draw :as draw]
            [golf.websocket :as ws]))

(defn nav-link [uri title page]
  [:a.navbar-item
   {:href   uri
    :class (when (= page @(rf/subscribe [:common/page])) :is-active)}
   title])

(defn navbar []
  (r/with-let [expanded? (rf/subscribe [:navbar-expanded?])]
              [:nav.navbar.is-info>div.container
     [:div.navbar-brand
      [:a.navbar-item {:href "/" :style {:font-weight :bold}} "golf"]
      [:span.navbar-burger.burger
       {:data-target :nav-menu
        :on-click #(do (rf/dispatch [:toggle-navbar-expanded])
                       (.stopPropagation %))
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
  (r/with-let
    [name (r/atom nil)]
    [:div.login-form
     [:h2 "Login"]
     [user-name-input {:value @name
                       :on-change #(reset! name (-> % .-target .-value))}]
     [login-button {:on-click #(if-let [name @name]
                                 (ws/send-login! name))}]]))

(defn info-display [name]
  [:div.info-display
   [:p "Logged in as " name]
   (if-let [game-id @(rf/subscribe [:game/id])]
     [:p "Connected to game " game-id])])

(defn logout-button [user-id]
  [:input.logout-button {:type "button"
                         :value "Logout"
                         :on-click #(ws/send-logout!)}])

(defn new-game-button []
  [:input.new-game-button {:type :button
                           :value "Create game"
                           :on-click #(ws/send-new-game!)}])

(defn refresh-games-button []
  [:input.refresh-games-button {:type :button
                                :value "Refresh"
                                :on-click #(ws/send-get-games!)}])

(defn game-list []
  (let [games @(rf/subscribe [:games])]
    [:ul.game-list
     (for [game games]
       ^{:key (:id game)}
       [:li {:on-click #(ws/send-join-game! (:id game))}
        (:id game)])]))

(defn login-page []
  [login-form])

(defn about-page []
  [:img {:src "/img/warning_clojure.png"}])

(defn game-page []
  [:div.game-page
   [:h2 "Games"]
   [game-list]
   [refresh-games-button]
   [:div]
   [new-game-button]])

(defn home-page []
  [:div.home-page
   [:h2 "Let's play golf."]
   (if @(rf/subscribe [:game])
     [draw/game-canvas])])

(defn page []
  (if-let [page @(rf/subscribe [:common/page])]
    [:div
     [navbar]
     [:section.section>div.container>div.content
      [page]
      (if-let [{:keys [id name]} @(rf/subscribe [:user])]
        [:div
         [info-display name]
         [logout-button id]])
      (if-let [game @(rf/subscribe [:game])]
        [:p (-> game
                (dissoc :deck)
                (update-in [:players] vals)
                str)])]]))
