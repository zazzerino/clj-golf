(ns golf.game-test
  (:require [clojure.test :refer :all]
            [clojure.spec.alpha :as spec]
            [golf.game :refer :all :as game]))

(deftest test-game
  (testing "create deck"
    (is (spec/valid? ::game/deck (make-deck)))
    (is (= 52 (count (make-deck)))))

  (testing "deal cards from a deck"
    (let [deck (make-deck)
          {:keys [card deck]} (deal-card deck)]
      (is (= card {:rank :ace :suit :clubs}))
      (is (= 51 (count deck)))))

  (testing "create player"
    (is (spec/valid? ::game/player (make-player "abcd1234" "Bob"))))

  (testing "add players to game"
    (let [g (make-game)
          p1 (make-player "1234" "Alice")
          p2 (make-player "56789" "Bob")]
      (is (spec/valid? ::game/game g))
      (is (empty? (:players g)))
      (is (nil? (:table-card g)))
      (let [g (add-player g p1)]
        (is (= 1 (count (:players g))))
        (is (= p1 (get (:players g) "1234")))
        (is (spec/valid? ::game/game g))
        (let [g (add-player g p2)]
          (is (= 2 (count (:players g))))
          (is (= p2 (get (:players g) "56789")))))))

  (testing "deal cards to players"
    (let [player-id "a1b2c3"
          p (make-player player-id "Charlie")
          g (-> (make-game)
                (add-player p)
                (deal-card-to-player player-id))]
      (is (= 51 (count (:deck g))))
      (is (spec/valid? :player/hand (get-in g [:players player-id :hand])))
      (is (= {:rank :ace :suit :clubs} (first (get-in g [:players player-id :hand]))))))

  (testing "init game"
    (is (spec/valid? ::game/game (init-game (make-game)
                                            [(make-player "id0" "name0")
                                             (make-player "id1" "name1")
                                             (make-player "id2" "name2")])))))
