(ns golf.game-test
  (:require [clojure.test :refer :all]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [golf.game :refer :all :as game]))

(deftest test-game
  (testing "make-deck"
    (let [deck (make-deck)]
      (is (s/valid? ::game/deck deck))
      (is (= 52 (count deck)))))

  (testing "deal-card"
    (let [deck (make-deck)
          {:keys [card deck]} (deal-card deck)]
      (is (s/valid? ::game/card card))
      (is (s/valid? ::game/deck deck))
      (is (= card {:rank :ace :suit :clubs}))
      (is (= 51 (count deck)))))

  (testing "make-player"
    (let [player (make-player "id0" "Bob")]
      (is (s/valid? ::game/player player))
      (is (= "id0" (:id player)))
      (is (= "Bob" (:name player)))
      (is (= "anon" (:name (make-player "id1"))))))

  (testing "make-game"
    (let [game (make-game)]
      (s/valid? ::game/game game)))

  (testing "add-player, add-players"
    (let [p1 (make-player "id0")
          p2 (make-player "id1" "Bob")
          game0 (-> (make-game)
                    (add-player p1)
                    (add-player p2))
          game1 (add-players (make-game) [p1 p2])]
      (is (s/valid? ::game/game game0))
      (is (= 0 (get-in game0 [:players "id0" :turn])))
      (is (= 1 (get-in game0 [:players "id1" :turn])))
      (is (= (dissoc game0 :id) (dissoc game1 :id)))))

  (testing "deal-card-to-player"
    (let [player-id "a1b2c3"
          player (make-player player-id "Charlie")
          game (-> (make-game)
                   (add-player player)
                   (deal-card-to-player player-id))]
      (is (= 51 (count (:deck game))))
      (is (s/valid? ::game/hand (get-in game [:players player-id :hand])))
      (is (= {:rank :ace :suit :clubs} (first (get-in game [:players player-id :hand]))))))

  (testing "give-card"
    (let [card {:rank :ace :suit :spades}
          p1 (make-player "id0")
          p2 (give-card p1 card)]
      (is (empty? (:hand p1)))
      (is (= card (get-in p2 [:hand 0])))))

  (testing "deal-card-to-player"
    (let [game (-> (make-game)
                   (add-player (make-player "id0"))
                   (deal-card-to-player "id0"))]
      (is (s/valid? ::game/game game))
      (is (= {:rank :ace :suit :clubs} (get-in game [:players "id0" :hand 0])))))

  (testing "deal-starting-hands"
    (let [game0 (make-game)
          p1-hand (->> game0 :deck (take-nth 2) (take 6))
          p2-hand (->> game0 :deck (drop 1) (take-nth 2) (take 6))
          game1 (-> game0
                    (add-players [(make-player "id0")
                                  (make-player "id1")])
                    (deal-starting-hands))]
      (is (s/valid? ::game/game game1))
      (is (= p1-hand (get-in game1 [:players "id0" :hand])))
      (is (= p2-hand (get-in game1 [:players "id1" :hand])))))

  (testing "deal-table-card"
    (let [game (-> (make-game)
                   (add-players [(make-player "id0")
                                 (make-player "id1")])
                   (deal-table-card))]
      (is (s/valid? ::game/game game))
      (is (= (:table-card game) {:rank :ace :suit :clubs}))))

  (testing "shuffle-deck"
    (let [game (-> (make-game)
                   (shuffle-deck))]
      (is (s/valid? ::game/game game))))

  (testing "start-game"
    (let [game (-> (make-game)
                   (start-game [(make-player "id0" "name0")
                                (make-player "id1" "name1")
                                (make-player "id2" "name2")]))]
      (is (s/valid? ::game/game game))
      (is ((comp not nil?) (:table-card game)))
      (is (true? (:started? game)))
      (is (= 0 (:round game)))))

  (testing "replace-card"
    (let [hand0 [{:rank :ace, :suit :hearts}
                 {:rank :2, :suit :diamonds}
                 {:rank :9, :suit :clubs}
                 {:rank :9, :suit :spades}
                 {:rank :6, :suit :clubs}
                 {:rank :10, :suit :spades}]
          card {:rank :3 :suit :clubs}
          hand1 (replace-card hand0 {:rank :2 :suit :diamonds} card)]
      (is (s/valid? ::game/hand hand0))
      (is (not= hand0 hand1))
      (is (= card (get hand1 1)))))

  (testing "get-hand"
    (let [game (-> (make-game)
                   (start-game [(make-player "id0")
                                (make-player "id1")]))]
      (is (s/valid? ::game/hand (get-hand game "id0")))
      (is (not= (get-hand game "id0") (get-hand game "id1")))))

  (testing "take-from-table"
    (let [game0 (-> (make-game)
                    (start-game [(make-player "id0")
                                 (make-player "id1")]))
          first-card (first (get-hand game0 "id0"))
          table-card (:table-card game0)
          game1 (take-from-table game0 "id0" first-card)]
      (is (s/valid? ::game/game game1))
      (is (= table-card (get-in game1 [:players "id0" :hand 0])))
      (is (= first-card (:table-card game1)))))

  (testing "take-from-deck"
    (let [game0 (-> (make-game)
                    (start-game [(make-player "id0")
                                 (make-player "id1")]))
          first-card (first (get-hand game0 "id0"))
          deck-card (-> game0 :deck first)
          game1 (take-from-deck game0 "id0" first-card)]
      (is (s/valid? ::game/game game1))
      (is (= deck-card (get-in game1 [:players "id0" :hand 0])))
      (is (= first-card (:table-card game1)))
      (is (not= (-> game0 :deck first)
                (-> game1 :deck first)))))

  (testing "player-move"
    ; take from table
    (let [game0 (-> (make-game)
                    (start-game [(make-player "id0")
                                 (make-player "id1")]))
          second-card (get-in game0 [:players "id0" :hand 1])
          game1 (player-move game0 "id0" :table second-card)]
      (is (s/valid? ::game/game game1))
      (is (= second-card (:table-card game1)))
      (is (= (:table-card game0) (get-in game1 [:players "id0" :hand 1]))))

    ; take from deck
    (let [game0 (-> (make-game)
                    (start-game [(make-player "id0")
                                 (make-player "id1")]))
          second-card (get-in game0 [:players "id0" :hand 1])
          game1 (player-move game0 "id0" :deck second-card)]
      (is (s/valid? ::game/game game1))
      (is (= second-card (:table-card game1)))
      (is (= (-> game0 :deck first) (get-in game1 [:players "id0" :hand 1])))))

  (testing "score"
    ; outer four matches
    (let [hand [{:rank :ace :suit :hearts}
                {:rank :nine :suit :clubs}
                {:rank :ace :suit :diamonds}
                {:rank :ace :suit :clubs}
                {:rank :five :suit :hearts}
                {:rank :ace :suit :spades}]]
      (is (= -6 (score hand))))

    ; left four matches
    (let [hand [{:rank :ace :suit :hearts}
                {:rank :ace :suit :clubs}
                {:rank :two :suit :diamonds}
                {:rank :ace :suit :diamonds}
                {:rank :ace :suit :spades}
                {:rank :jack :suit :spaces}]]
      (is (= 2 (score hand))))

    ; right four matches
    (let [hand [{:rank :two :suit :hearts}
                {:rank :nine :suit :clubs}
                {:rank :nine :suit :diamonds}
                {:rank :five :suit :clubs}
                {:rank :nine :suit :hearts}
                {:rank :nine :suit :spades}]]
      (is (= -3 (score hand))))

    ; right column matches
    (let [hand [{:rank :four :suit :hearts}
                {:rank :nine :suit :clubs}
                {:rank :ace :suit :diamonds}
                {:rank :two :suit :clubs}
                {:rank :five :suit :hearts}
                {:rank :ace :suit :spades}]]
      (is (= 20 (score hand))))

    ; middle column matches
    (let [hand [{:rank :ace :suit :hearts}
                {:rank :five :suit :clubs}
                {:rank :seven :suit :diamonds}
                {:rank :jack :suit :clubs}
                {:rank :five :suit :hearts}
                {:rank :ace :suit :spades}]]
      (is (= 19 (score hand))))

    ; left column matches
    (let [hand [{:rank :ace :suit :hearts}
                {:rank :nine :suit :clubs}
                {:rank :eight :suit :diamonds}
                {:rank :ace :suit :clubs}
                {:rank :five :suit :hearts}
                {:rank :four :suit :spades}]]
      (is (= 26 (score hand))))

    ; no matches
    (let [hand [{:rank :ace :suit :hearts}
                {:rank :nine :suit :clubs}
                {:rank :two :suit :diamonds}
                {:rank :three :suit :clubs}
                {:rank :five :suit :hearts}
                {:rank :queen :suit :spades}]]
      (is (= 30 (score hand))))
    )
  )
