(ns golf.manager
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.set :as set]
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

(defn create-user!
  ([ctx id name]
   (let [user (make-user id name)]
     (swap! ctx update-in [:users] conj {id user})
     user))
  ([ctx id]
   (create-user! ctx id "anon")))

(defn remove-user! [ctx id]
  (swap! ctx update-in [:users] dissoc id))

(defn login! [ctx id name]
  (swap! ctx assoc-in [:users id :name] name))

(defn logout! [ctx id]
  (swap! ctx assoc-in [:users id :name] "anon"))

(defn get-user-by-id [ctx id]
  (get-in @ctx [:users id]))

;; games

(defn create-game! [ctx]
  (let [game (game/make-game)]
    (swap! ctx update-in [:games] conj {(:id game) game})
    game))

(defn remove-game! [ctx game-id]
  (swap! ctx update-in [:games] dissoc game-id))

(defn get-game-by-id [ctx id]
  (get-in @ctx [:games id]))

(defn join-game! [ctx game-id user-id]
  (let [user (-> (get-user-by-id ctx user-id)
                 (assoc :game-id game-id))
        game (-> (get-game-by-id ctx game-id)
                 (game/add-player (game/make-player user-id (:name user))))]
    (swap! ctx #(-> %
                    (assoc-in [:users user-id] user)
                    (assoc-in [:games game-id] game)))
    game))

(defn join-new-game! [ctx user-id]
  (let [game (create-game! ctx)
        game-id (:id game)]
    (join-game! ctx game-id user-id)
    (get-game-by-id ctx game-id)))

(defn start-game! [ctx id]
  (let [game (-> (get-game-by-id ctx id)
                 (game/start-game))]
    (swap! ctx assoc-in [:games id] game)))

(defn- prettify-game [game]
  (-> (select-keys game [:id :players])
      (update-in [:players] vals)))

(defn get-all-games [ctx]
  (->> @ctx :games vals (map prettify-game)))

(defn get-player-ids [ctx game-id]
  (keys (get-in @ctx [:games game-id :players])))

(defn has-active-players? [ctx game-id]
  (let [user-ids (-> @ctx :users keys)
        player-ids (get-player-ids ctx game-id)]
    (not (empty? (set/intersection (set user-ids) (set player-ids))))))

(defn remove-inactive-games! [ctx]
  (doseq [game-id (-> @ctx :games keys)]
    (when-not (has-active-players? ctx game-id)
      (remove-game! ctx game-id))))
