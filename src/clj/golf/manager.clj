(ns golf.manager
  (:require [clojure.spec.alpha :as s]
            ;[clojure.spec.gen.alpha :as gen]
            [golf.game :as game])
  (:import (java.util UUID)
           (org.projectodd.wunderboss.web.undertow.async.websocket UndertowWebsocketChannel)))

(s/def ::id (s/and string? (comp not empty?)))
(s/def ::name (s/and string? (comp not empty?)))
(s/def ::channel #(instance? UndertowWebsocketChannel %))
(s/def ::user (s/keys :req-un [::id ::name ::channel]))

(s/def ::users (s/map-of ::id ::user))
(s/def ::games (s/map-of ::id ::game/game))
(s/def ::context (s/keys :req-un [::users ::games]))

(defn make-context []
  (atom {:users {}
         :games {}}))

(def context (make-context))

(defn- uuid []
  (str (UUID/randomUUID)))

;; users

(defn make-user
  ([channel name]
   {:id (uuid)
    :channel channel
    :name name})
  ([channel]
   (make-user channel "anon")))

(defn add-user! [context user]
  (swap! context update-in [:users] conj {(:id user) user}))

(defn remove-user! [context id]
  (swap! context update-in [:users] dissoc id))

(defn login! [context id name]
  (swap! context assoc-in [:users id :name] name))

(defn logout! [context id]
  (swap! context assoc-in [:users id :name] "anon"))

(defn get-user-by-id [context id]
  (get (:users @context) id))

(defn get-user-by-channel [context channel]
  (let [users (-> @context :users vals)]
    (first (filter #(= channel (:channel %)) users))))

;; games

(def make-game game/make-game)

(defn add-game! [context game]
  (swap! context update-in [:games] conj {(:id game) game}))

(defn remove-game! [context id]
  (swap! context update-in [:games] dissoc id))

(defn get-game-by-id [context id]
  (get-in @context [:games id]))

(defn get-games [context]
  (-> @context :games vals))

(defn connect-user-to-game! [context user-id game-id]
  (let [user (-> (get-user-by-id context user-id)
                 (assoc :game-id game-id))
        player (game/make-player user-id (:name user))
        game (-> (get-game-by-id context game-id)
                 (game/add-player player))]
    (swap! context #(-> (assoc-in % [:users user-id] user)
                        (assoc-in [:games game-id] game)))))

(defn start-game! [context id]
  (let [game (-> (get-game-by-id context id)
                 (game/start-game))]
    (swap! context assoc-in [:games id] game)))