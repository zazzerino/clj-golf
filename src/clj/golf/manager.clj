(ns golf.manager
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [golf.game :as game]))

(s/def ::id (s/and string? (comp not empty?)))
(s/def ::user (s/keys :req-un [::id]))
(s/def ::users (s/map-of ::id ::user))
(s/def ::games (s/map-of ::id ::game/game))
(s/def ::context (s/keys :req-un [::users ::games]))

(defn make-context []
  (atom {:users {}
         :games {}}))

;; users

(defn make-user
  ([id name]
   {:id id
    :name name})
  ([id]
   (make-user id "anon")))

(defn set-name [user name]
  (assoc user :name name))

(defn add-user [ctx id]
  (let [user (make-user id)]
    (swap! ctx update-in [:users] conj {id user})))

(defn remove-user [ctx id]
  (swap! ctx update-in [:users] dissoc id))

(defn login [ctx id name]
  (swap! ctx assoc-in [:users id :name] name))

(defn logout [ctx id]
  (swap! ctx assoc-in [:users id :name] "anon"))

(defn get-user-by-id [ctx id]
  (get-in @ctx [:users id]))

;; games

(defn new-game [ctx user-id]
  (let [user (get-user-by-id ctx user-id)
        name (if (:name user) (:name user) "anon")
        game (-> (game/make-game)
                 (game/add-player (game/make-player user-id name)))
        game-id (:id game)
        user (assoc user :game-id game-id)]
    (swap! ctx #(-> %
                    (assoc-in [:users user-id] user)
                    (assoc-in [:games game-id] game)))
    game))

(defn add-game [ctx game]
  (swap! ctx update-in [:games] conj {(:id game) game}))

(defn remove-game [ctx game]
  (swap! ctx update-in [:games] dissoc (:id game)))

(defn get-game-by-id [ctx id]
  (get-in @ctx [:games id]))

(defn join-game [ctx game-id user-id]
  (let [user (-> (get-user-by-id ctx user-id)
                 (assoc :game-id game-id))
        game (-> (get-game-by-id ctx game-id)
                 (game/add-player (game/make-player user-id (:name user))))]
    (swap! ctx #(-> %
                    (assoc-in [:users user-id] user)
                    (assoc-in [:games game-id] game)))
    game))

(defn start-game [ctx id]
  (let [game (-> (get-game-by-id ctx id)
                 (game/start-game))]
    (swap! ctx assoc-in [:games id] game)))

(defn prettify-game [game]
  (-> (select-keys game [:id :players])
      (update-in [:players] vals)))

(defn get-all-games [ctx]
  (->> @ctx :games vals (map prettify-game)))

(defn get-player-ids [ctx game-id]
  (keys (get-in @ctx [:games game-id :players])))
