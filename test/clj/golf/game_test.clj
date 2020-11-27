(ns golf.game-test
  (:require [clojure.test :refer :all]
            [clojure.spec.alpha :as s]
            [golf.game :refer :all :as game]))

(deftest test-game
  (testing "create a deck"
    (let [deck (make-deck)]
      (is (s/valid? ::game/deck deck))
      (is (= 52 (count deck)))))

  (testing "deal cards from a deck"
    (let [deck (make-deck)
          {:keys [card deck]} (deal-card deck)]
      (is (s/valid? ::game/card card))
      (is (s/valid? ::game/deck deck))
      (is (= card {:rank :ace :suit :clubs}))
      (is (= 51 (count deck)))))

  (testing "create a player"
    (let [player (make-player "abcd1234" "Bob")]
    (is (s/valid? ::game/player player))
    (is (= "abcd1234" (:id player)))
    (is (= "Bob" (:name player)))
    (is (= "anon" (:name (make-player "id1"))))))

  (testing "make a game"
    (let [game (make-game)]
      (s/valid? ::game/game game)))

  (testing "add players"
    (let [p1 (make-player "id0")
          p2 (make-player "id1" "Bob")
          game0 (-> (make-game)
                    (add-player p1)
                    (add-player p2))
          game1 (add-players (make-game) [p1 p2])]
      (is (s/valid? ::game/game game0))
      (is (= 1 (get-in game0 [:players "id0" :turn])))
      (is (= 2 (get-in game0 [:players "id1" :turn])))
      (is (= (dissoc game0 :id) (dissoc game1 :id)))))

  (testing "deal cards to players"
    (let [player-id "a1b2c3"
          p (make-player player-id "Charlie")
          g (-> (make-game)
                (add-player p)
                (deal-card-to-player player-id))]
      (is (= 51 (count (:deck g))))
      (is (s/valid? ::game/hand (get-in g [:players player-id :hand])))
      (is (= {:rank :ace :suit :clubs} (first (get-in g [:players player-id :hand]))))))

  (testing "init game"
    (is (s/valid? ::game/game (start-game (make-game)
                                          [(make-player "id0" "name0")
                                           (make-player "id1" "name1")
                                           (make-player "id2" "name2")]))))

  (testing "take table card"
    (let [game {:id "9227e3ef-26fb-42f4-bd0b-dbaf67b2d3e1",
                :players {"id0" {:id "id0",
                                 :name "name0",
                                 :hand [{:rank :ace, :suit :clubs}
                                        {:rank :5, :suit :hearts}
                                        {:rank :8, :suit :hearts}
                                        {:rank :queen, :suit :spades}
                                        {:rank :10, :suit :spades}
                                        {:rank :jack, :suit :diamonds}]},
                          "id1" {:id "id1",
                                 :name "name1",
                                 :hand [{:rank :3, :suit :hearts}
                                        {:rank :6, :suit :diamonds}
                                        {:rank :3, :suit :spades}
                                        {:rank :2, :suit :clubs}
                                        {:rank :4, :suit :clubs}
                                        {:rank :queen, :suit :diamonds}]}},
                :deck '({:rank :10, :suit :hearts}
                        {:rank :ace, :suit :hearts}
                        {:rank :9, :suit :diamonds}
                        {:rank :king, :suit :clubs}
                        {:rank :3, :suit :diamonds}
                        {:rank :2, :suit :diamonds}
                        {:rank :jack, :suit :hearts}
                        {:rank :6, :suit :spades}
                        {:rank :king, :suit :diamonds}
                        {:rank :queen, :suit :hearts}
                        {:rank :jack, :suit :spades}
                        {:rank :king, :suit :hearts}
                        {:rank :7, :suit :spades}
                        {:rank :2, :suit :spades}
                        {:rank :6, :suit :clubs}
                        {:rank :2, :suit :hearts}
                        {:rank :ace, :suit :spades}
                        {:rank :queen, :suit :clubs}
                        {:rank :7, :suit :hearts}
                        {:rank :ace, :suit :diamonds}
                        {:rank :8, :suit :diamonds}
                        {:rank :9, :suit :clubs}
                        {:rank :8, :suit :spades}
                        {:rank :6, :suit :hearts}
                        {:rank :4, :suit :spades}
                        {:rank :9, :suit :hearts}
                        {:rank :4, :suit :diamonds}
                        {:rank :3, :suit :clubs}
                        {:rank :7, :suit :diamonds}
                        {:rank :5, :suit :spades}
                        {:rank :jack, :suit :clubs}
                        {:rank :king, :suit :spades}
                        {:rank :8, :suit :clubs}
                        {:rank :5, :suit :clubs}
                        {:rank :4, :suit :hearts}
                        {:rank :5, :suit :diamonds}
                        {:rank :10, :suit :clubs}
                        {:rank :7, :suit :clubs}
                        {:rank :10, :suit :diamonds}),
                :turn 0,
                :table-card {:rank :9, :suit :spades}}]
      (is (s/valid? ::game/game game))
      (let [game' (take-from-table game "id0" {:rank :5 :suit :hearts})]
        (is (s/valid? ::game/game game'))
        (is (= (:table-card game') {:rank :5 :suit :hearts}))
        (is (= {:rank :9 :suit :spades} (get-in game' [:players "id0" :hand 1]))))))

  (testing "take table card"
    (let [game {:id "9227e3ef-26fb-42f4-bd0b-dbaf67b2d3e1",
                :players {"id0" {:id "id0",
                                 :name "name0",
                                 :hand [{:rank :ace, :suit :clubs}
                                        {:rank :5, :suit :hearts}
                                        {:rank :8, :suit :hearts}
                                        {:rank :queen, :suit :spades}
                                        {:rank :10, :suit :spades}
                                        {:rank :jack, :suit :diamonds}]},
                          "id1" {:id "id1",
                                 :name "name1",
                                 :hand [{:rank :3, :suit :hearts}
                                        {:rank :6, :suit :diamonds}
                                        {:rank :3, :suit :spades}
                                        {:rank :2, :suit :clubs}
                                        {:rank :4, :suit :clubs}
                                        {:rank :queen, :suit :diamonds}]}},
                :deck '({:rank :10, :suit :hearts}
                        {:rank :ace, :suit :hearts}
                        {:rank :9, :suit :diamonds}
                        {:rank :king, :suit :clubs}
                        {:rank :3, :suit :diamonds}
                        {:rank :2, :suit :diamonds}
                        {:rank :jack, :suit :hearts}
                        {:rank :6, :suit :spades}
                        {:rank :king, :suit :diamonds}
                        {:rank :queen, :suit :hearts}
                        {:rank :jack, :suit :spades}
                        {:rank :king, :suit :hearts}
                        {:rank :7, :suit :spades}
                        {:rank :2, :suit :spades}
                        {:rank :6, :suit :clubs}
                        {:rank :2, :suit :hearts}
                        {:rank :ace, :suit :spades}
                        {:rank :queen, :suit :clubs}
                        {:rank :7, :suit :hearts}
                        {:rank :ace, :suit :diamonds}
                        {:rank :8, :suit :diamonds}
                        {:rank :9, :suit :clubs}
                        {:rank :8, :suit :spades}
                        {:rank :6, :suit :hearts}
                        {:rank :4, :suit :spades}
                        {:rank :9, :suit :hearts}
                        {:rank :4, :suit :diamonds}
                        {:rank :3, :suit :clubs}
                        {:rank :7, :suit :diamonds}
                        {:rank :5, :suit :spades}
                        {:rank :jack, :suit :clubs}
                        {:rank :king, :suit :spades}
                        {:rank :8, :suit :clubs}
                        {:rank :5, :suit :clubs}
                        {:rank :4, :suit :hearts}
                        {:rank :5, :suit :diamonds}
                        {:rank :10, :suit :clubs}
                        {:rank :7, :suit :clubs}
                        {:rank :10, :suit :diamonds}),
                :turn 0,
                :table-card {:rank :9, :suit :spades}}]
      (is (s/valid? ::game/game game))
      (let [top-of-deck (first (:deck game))
            game' (take-from-deck game "id0" {:rank :5 :suit :hearts})]
        (is (s/valid? ::game/game game'))
        (is (= {:rank :5 :suit :hearts} (:table-card game')))
        (is (= (-> game' :deck count) (-> game :deck count dec)))
        (is (= top-of-deck (get-in game' [:players "id0" :hand 1])))
        )))

  (testing "player move"
    (let [game {:id "9227e3ef-26fb-42f4-bd0b-dbaf67b2d3e1",
                :players {"id0" {:id "id0",
                                 :name "name0",
                                 :hand [{:rank :ace, :suit :clubs}
                                        {:rank :5, :suit :hearts}
                                        {:rank :8, :suit :hearts}
                                        {:rank :queen, :suit :spades}
                                        {:rank :10, :suit :spades}
                                        {:rank :jack, :suit :diamonds}]},
                          "id1" {:id "id1",
                                 :name "name1",
                                 :hand [{:rank :3, :suit :hearts}
                                        {:rank :6, :suit :diamonds}
                                        {:rank :3, :suit :spades}
                                        {:rank :2, :suit :clubs}
                                        {:rank :4, :suit :clubs}
                                        {:rank :queen, :suit :diamonds}]}},
                :deck '({:rank :10, :suit :hearts}
                        {:rank :ace, :suit :hearts}
                        {:rank :9, :suit :diamonds}
                        {:rank :king, :suit :clubs}
                        {:rank :3, :suit :diamonds}
                        {:rank :2, :suit :diamonds}
                        {:rank :jack, :suit :hearts}
                        {:rank :6, :suit :spades}
                        {:rank :king, :suit :diamonds}
                        {:rank :queen, :suit :hearts}
                        {:rank :jack, :suit :spades}
                        {:rank :king, :suit :hearts}
                        {:rank :7, :suit :spades}
                        {:rank :2, :suit :spades}
                        {:rank :6, :suit :clubs}
                        {:rank :2, :suit :hearts}
                        {:rank :ace, :suit :spades}
                        {:rank :queen, :suit :clubs}
                        {:rank :7, :suit :hearts}
                        {:rank :ace, :suit :diamonds}
                        {:rank :8, :suit :diamonds}
                        {:rank :9, :suit :clubs}
                        {:rank :8, :suit :spades}
                        {:rank :6, :suit :hearts}
                        {:rank :4, :suit :spades}
                        {:rank :9, :suit :hearts}
                        {:rank :4, :suit :diamonds}
                        {:rank :3, :suit :clubs}
                        {:rank :7, :suit :diamonds}
                        {:rank :5, :suit :spades}
                        {:rank :jack, :suit :clubs}
                        {:rank :king, :suit :spades}
                        {:rank :8, :suit :clubs}
                        {:rank :5, :suit :clubs}
                        {:rank :4, :suit :hearts}
                        {:rank :5, :suit :diamonds}
                        {:rank :10, :suit :clubs}
                        {:rank :7, :suit :clubs}
                        {:rank :10, :suit :diamonds}),
                :turn 0,
                :table-card {:rank :9, :suit :spades}}]
      (let [game' (player-move game {:player-id "id1"
                                     :card-source :table
                                     :card-to-replace {:rank :2 :suit :clubs}})]
        (is (s/valid? ::game/game game'))
        (is (= 1 (:turn game')))
        (is (= {:rank :9 :suit :spades} (get-in game' [:players "id1" :hand 3])))
        (is (= {:rank :2 :suit :clubs} (:table-card game'))))
      (let [game' (player-move game {:player-id "id0"
                                     :card-source :deck
                                     :card-to-replace {:rank :jack :suit :diamonds}})]
        (is (s/valid? ::game/game game'))
        (is (= 1 (:turn game')))
        (is (= (:table-card game') {:rank :jack :suit :diamonds}))
        (is (= {:rank :10 :suit :hearts} (get-in game' [:players "id0" :hand 5])))
        )))
  )
