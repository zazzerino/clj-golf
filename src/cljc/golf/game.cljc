(ns golf.game
  (:require [clojure.spec.alpha :as spec])
  #?(:clj (:import (java.util UUID)))
  )

(def ranks [:ace :2 :3 :4 :5 :6 :7 :8 :9 :10 :jack :queen :king])
(def suits [:clubs :diamonds :hearts :spades])

(spec/def ::rank (set ranks))
(spec/def ::suit (set suits))
(spec/def ::card (spec/keys :req-un [::rank ::suit]))
(spec/def ::deck (spec/* ::card))

(spec/def :player/id string?)
(spec/def :player/name string?)
(spec/def :player/hand (spec/* ::card))
(spec/def ::player (spec/keys :req-un [:player/id :player/name :player/hand]))
(spec/def ::players (spec/map-of :player/id ::player))

(spec/def ::table-card (spec/nilable ::card))
(spec/def ::game (spec/keys :req-un [::players ::deck ::table-card]))

(defn make-deck []
  (for [rank ranks
        suit suits]
    {:rank rank
     :suit suit}))

(defn deal-card [deck]
  {:card (first deck)
   :deck (rest deck)})

(defn make-player [id name]
  {:id   id
   :name name
   :hand []})

(defn make-game []
  {#?@(:clj [:id (str (UUID/randomUUID))]
       :cljs [:id (str (random-uuid))])
   :players    {}
   :deck       (make-deck)
   :table-card nil})

(defn add-player [game player]
  (update-in game [:players] conj {(:id player) player}))

(defn add-players [game players]
  (reduce (fn [game player]
            (add-player game player))
          game
          players))

(defn give-card [player card]
  (update-in player [:hand] conj card))

(defn deal-card-to-player [game player-id]
  (let [{:keys [card deck]} (deal-card (:deck game))
        player (-> (get (:players game) player-id)
                   (give-card card))]
    (-> (assoc-in game [:players player-id] player)
        (assoc :deck deck))))

(defn deal-starting-hands [game]
  (let [hand-size 6
        num-players (count (:players game))]
    (loop [cards-remaining (* hand-size num-players)
           player-ids (cycle (keys (:players game)))
           game game]
      (if (zero? cards-remaining)
        game
        (recur (dec cards-remaining)
               (rest player-ids)
               (deal-card-to-player game (first player-ids)))))))

(defn deal-table-card [game]
  (let [{:keys [card deck]} (deal-card (:deck game))]
    (assoc game :table-card card
                :deck deck)))

(defn shuffle-deck [game]
  (update-in game [:deck] shuffle))

(defn init-game [game players]
  (-> game
      (add-players players)
      (shuffle-deck)
      (deal-table-card)
      (deal-starting-hands)))
