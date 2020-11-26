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
        game (-> (game/make-game)
                 (game/add-player (game/make-player user-id)))
        game-id (:id game)
        user (assoc user :game-id game-id)]
    (swap! ctx #(-> %
                    (assoc-in [:users user-id] user)
                    (assoc-in [:games game-id] game)))
    game))

#_(defn add-game [ctx game]
  (swap! ctx update-in [:games] conj {(:id game) game}))

(defn remove-game [ctx game]
  (swap! ctx update-in [:games] dissoc (:id game)))

(defn get-game-by-id [ctx id]
  (get-in @ctx [:games id]))

(defn connect-user-to-game [ctx user-id game-id]
  (let [user (-> (get-user-by-id ctx user-id)
                 (assoc :game-id game-id))
        game (-> (get-game-by-id ctx game-id)
                 (game/add-player (game/make-player user-id)))]
    (swap! ctx #(-> %
                    (assoc-in [:users user-id] user)
                    (assoc-in [:games game-id] game)))))

(defn start-game [ctx id]
  (let [game (-> (get-game-by-id ctx id)
                 (game/start-game))]
    (swap! ctx assoc-in [:games id] game)))

;; ------------------------------------------------------------------------

;(s/def ::id (s/and string? (comp not empty?)))
;(s/def ::name (s/and string? (comp not empty?)))
;(s/def ::channel #(instance? UndertowWebsocketChannel %))
;(s/def ::user (s/keys :req-un [::id ::name ::channel]))
;
;(s/def ::users (s/map-of ::id ::user))
;(s/def ::games (s/map-of ::id ::game/game))
;(s/def ::context (s/keys :req-un [::users ::games]))
;
;(defn make-context []
;  (atom {:users {}
;         :games {}}))
;
;(def context (make-context))
;
;(defn- uuid []
;  (str (UUID/randomUUID)))
;
;;; users
;
;(defn make-user
;  ([channel name]
;   {:id (uuid)
;    :channel channel
;    :name name})
;  ([channel]
;   (make-user channel "anon")))
;
;(defn add-user! [context user]
;  (swap! context update-in [:users] conj {(:id user) user}))
;
;(defn remove-user! [context id]
;  (swap! context update-in [:users] dissoc id))
;
;(defn login! [context id name]
;  (swap! context assoc-in [:users id :name] name))
;
;(defn logout! [context id]
;  (swap! context assoc-in [:users id :name] "anon"))
;
;(defn get-user-by-id [context id]
;  (get (:users @context) id))
;
;(defn get-user-by-channel [context channel]
;  (let [users (-> @context :users vals)]
;    (first (filter #(= channel (:channel %)) users))))
;
;;; games
;
;(def make-game game/make-game)
;
;(defn add-game! [context game]
;  (swap! context update-in [:games] conj {(:id game) game}))
;
;(defn remove-game! [context id]
;  (swap! context update-in [:games] dissoc id))
;
;(defn get-game-by-id [context id]
;  (get-in @context [:games id]))
;
;(defn get-games [context]
;  (-> @context :games vals))
;
;(defn connect-user-to-game! [context user-id game-id]
;  (let [user (-> (get-user-by-id context user-id)
;                 (assoc :game-id game-id))
;        player (game/make-player user-id (:name user))
;        game (-> (get-game-by-id context game-id)
;                 (game/add-player player))]
;    (swap! context #(-> (assoc-in % [:users user-id] user)
;                        (assoc-in [:games game-id] game)))))
;
;(defn start-game! [context id]
;  (let [game (-> (get-game-by-id context id)
;                 (game/start-game))]
;    (swap! context assoc-in [:games id] game)))
