(ns golf.views
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [golf.websocket :as websocket]))

;; (defonce messages (atom []))

;; (defn message-list []
;;   [:ul
;;    [:h4 "message list"]
;;    (for [[i message] (map-indexed vector @messages)]
;;      ^{:key i}
;;      [:li message])])

;; (defn message-input []
;;   (let [value (atom nil)]
;;     (fn []
;;       [:input.form-control
;;        {:type :text
;;         :placeholder "enter message here"
;;         :value @value
;;         :on-change #(reset! value (-> % .-target .-value))
;;         :on-key-down #(when (= (.-keyCode %) 13)
;;                         (ws/send-transit-msg! {:message @value})
;;                         (reset! value nil))}])))

;(defn update-messages! [{:keys [message]}]
;  (swap! messages #(vec (take 10 (conj % message)))))

(defn nav-link [uri title page]
  [:a.navbar-item
   {:href   uri
    :class (when (= page @(re-frame/subscribe [:common/page])) :is-active)}
   title])

(defn navbar []
  (reagent/with-let [expanded? (reagent/atom false)]
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

(defn about-page []
  [:section.section>div.container>div.content
   [:img {:src "/img/warning_clojure.png"}]])

(defn home-page []
  [:section.section>div.container>div.content
   [login-form]
   ;; [message-list]
   ;; [message-input]
   #_(when-let [docs @(re-frame/subscribe [:docs])]
     [:div {:dangerouslySetInnerHTML {:__html (md->html docs)}}])])

(defn page []
  (if-let [page @(re-frame/subscribe [:common/page])]
    [:div
     [navbar]
     [page]]))
