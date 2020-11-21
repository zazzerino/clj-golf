(ns golf.views
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [golf.websocket :as websocket]))

(defn nav-link [uri title page]
  [:a.navbar-item
   {:href   uri
    :class (when (= page @(re-frame/subscribe [:common/page])) :is-active)}
   title])

(defn navbar []
  (reagent/with-let
    [expanded? (reagent/atom false)]
    [:nav.navbar.is-info>div.container
     [:div.navbar-brand
      [:a.navbar-item {:href "/" :style {:font-weight :bold}} "golf"]
      [:span.navbar-burger.burger
       {:data-target :nav-menu
        :on-click #(swap! expanded? not)
        :class (when @expanded? :is-active)}
       [:span][:span][:span]]]
     [:div#nav-menu.navbar-menu
      {:class (when @expanded? :is-active)}
      [:div.navbar-start
       [nav-link "#/" "Home" :home]
       [nav-link "#/login" "Login" :login]
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
                                 (websocket/send-login! {:name @name}))}]]))

(defn user-display []
  (if-let [name @(re-frame/subscribe [:user/name])]
    [:div.user-display (str "logged in as " name)]))

(defn login-page []
  [login-form])

(defn about-page []
  [:img {:src "/img/warning_clojure.png"}])

(defn home-page []
  [:h2 "Let's play golf."])

(defn page []
  (if-let [page @(re-frame/subscribe [:common/page])]
    [:div
     [navbar]
     [:section.section>div.container>div.content
      [page]
      [user-display]]]))
