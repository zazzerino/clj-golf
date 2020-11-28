(ns golf.game
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen])
  #?(:clj (:import (java.util UUID))))

(def ranks [:ace :2 :3 :4 :5 :6 :7 :8 :9 :10 :jack :queen :king])
(def suits [:clubs :diamonds :hearts :spades])

(s/def ::rank (set ranks))
(s/def ::suit (set suits))
(s/def ::card (s/keys :req-un [::rank ::suit]))
(s/def ::deck (s/* ::card))

(s/def ::id string?)
(s/def ::name string?)
(s/def ::hand (s/coll-of ::card))
(s/def ::turn (s/and int? (comp not neg?)))
(s/def ::player (s/keys :req-un [::id ::name ::hand]
                        :opt-un [::turn]))
(s/def ::players (s/map-of ::id ::player))

(s/def ::table-card ::card)
(s/def ::scratch-card ::card)
(s/def ::card-source #{:table :deck})
(s/def ::round (s/and integer? (comp not neg?)))
(s/def ::started? boolean?)

(s/def ::game (s/keys :req-un [::players ::deck]
                      :opt-un [::table-card ::round ::started?]))

(defn- gen-uuid []
  #?(:clj (str (UUID/randomUUID))
     :cljs (str (random-uuid))))

(defn make-deck []
  (for [rank ranks
        suit suits]
    {:rank rank
     :suit suit}))

(defn deal-card [deck]
  {:card (first deck)
   :deck (rest deck)})

(defn make-player
  ([id name]
   {:id id
    :name name
    :hand []})
  ([id]
   (make-player id "anon")))

(defn make-game []
  {:id (gen-uuid)
   :players {}
   :deck (make-deck)})

(defn add-player [game player]
  (let [turn (-> game :players count)
        player (assoc player :turn turn)]
    (update-in game [:players] conj {(:id player) player})))

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

(defn start-game
  ([game]
   (-> game
       (assoc :started? true)
       (assoc :round 0)
       (shuffle-deck)
       (deal-starting-hands)
       (deal-table-card)))
  ([game players]
   (start-game (add-players game players))))

(defn replace-card [hand card-to-replace new-card]
  (replace {card-to-replace new-card} hand))

(defn get-hand [game player-id]
  (get-in game [:players player-id :hand]))

(defn take-from-table [game player-id card-to-replace]
  (let [table-card (:table-card game)
        hand (-> (get-hand game player-id)
                 (replace-card card-to-replace table-card))]
    (-> game
        (assoc-in [:players player-id :hand] hand)
        (assoc :table-card card-to-replace))))

(defn take-from-deck [game player-id card-to-replace]
  (let [{:keys [card deck]} (deal-card (:deck game))
        hand (-> (get-hand game player-id)
                 (replace-card card-to-replace card))]
    (-> game
        (assoc-in [:players player-id :hand] hand)
        (assoc :table-card card-to-replace)
        (assoc :deck deck))))

(defn player-move [game player-id card-source card-to-replace]
  (-> (case card-source
        :deck (take-from-deck game player-id card-to-replace)
        :table (take-from-table game player-id card-to-replace))
      (update :round inc)))

(defn players-by-turn [game]
  (sort-by :turn (-> game :players vals)))

(defn- shift [seq n]
  (->> (cycle seq) (drop n) (take (count seq))))

(defn hands-starting-at-turn [game turn]
  (-> (map :hand (players-by-turn game))
      (shift turn)))

(defn golf-value [rank]
  (let [rank-vals {:king 0
                   :ace 1
                   :two 2
                   :three 3
                   :four 4
                   :five 5
                   :six 6
                   :seven 7
                   :eight 8
                   :nine 9
                   :ten 10
                   :jack 10
                   :queen 10}]
    (get rank-vals rank)))

(def ^:private sum (partial reduce +))

(def h [{:rank :ace :suit :hearts}
        {:rank :nine :suit :clubs}
        {:rank :ace :suit :diamonds}
        {:rank :ace :suit :clubs}
        {:rank :five :suit :hearts}
        {:rank :ace :suit :spaces}])

(defn score [hand]
  (let [ranks (map :rank hand)
        [a b c d e f] ranks
        vals (map golf-value ranks)
        [a-val b-val c-val d-val e-val f-val] vals]
    (cond
      ; outer four match
      (= a c d f) (sum [b-val e-val -20])
      ; left four match
      (= a b d e) (sum [c-val f-val -10])
      ; right four match
      (= b c e f) (sum [a-val d-val -10])
      ; right match
      (= c f) (sum [a-val b-val d-val e-val])
      ; middle match
      (= b e) (sum [a-val c-val d-val f-val])
      ; left match
      (= a d) (sum [b-val c-val e-val f-val])
      :else (sum vals))))
