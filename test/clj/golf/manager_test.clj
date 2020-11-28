(ns golf.manager-test
  (:require [clojure.test :refer :all]
            [clojure.spec.alpha :as s]
            [golf.game :as game]
            [golf.manager :refer :all :as manager]))

(deftest test-manager
  (testing "make-context"
    (let [ctx (make-context)]
      (is (s/valid? ::manager/context @ctx))))

  (testing "make-user"
    (is (s/valid? ::manager/user (make-user "id0" "name0")))
    (is (s/valid? ::manager/user (make-user "id0")))
    (is (= "anon" (:name (make-user "id0")))))

  (testing "create-user!"
    (let [ctx (make-context)]
      (create-user! ctx "id0")
      (create-user! ctx "id1" "name1")
      (is (= 2 (-> @ctx :users count)))
      (is (= "anon" (get-in @ctx [:users "id0" :name])))
      (is (s/valid? ::manager/context @ctx))))

  (testing "remove-user!"
    (let [ctx (make-context)]
      (create-user! ctx "id0")
      (is (= 1 (-> @ctx :users count)))
      (remove-user! ctx "id0")
      (is (= 0 (-> @ctx :users count)))
      (is (s/valid? ::manager/context @ctx))))

  (testing "login!"
    (let [ctx (make-context)]
      (create-user! ctx "id0")
      (is (= "anon" (get-in @ctx [:users "id0" :name])))
      (login! ctx "id0" "Alice")
      (is (= "Alice" (get-in @ctx [:users "id0" :name])))
      (is (s/valid? ::manager/context @ctx))))

  (testing "logout!"
    (let [ctx (make-context)]
      (create-user! ctx "id0" "Bob")
      (is (= "Bob" (get-in @ctx [:users "id0" :name])))
      (logout! ctx "id0")
      (is (= "anon" (get-in @ctx [:users "id0" :name])))
      (is (s/valid? ::manager/context @ctx))))

  (testing "get-user-by-id"
    (let [ctx (make-context)]
      (create-user! ctx "id0")
      (is (= "id0" (:id (get-user-by-id ctx "id0"))))
      (is (= "anon" (:name (get-user-by-id ctx "id0"))))
      (is (s/valid? ::manager/user (get-user-by-id ctx "id0")))
      (is (s/valid? ::manager/context @ctx))))

  (testing "new-game!"
    (let [ctx (make-context)]
      (create-game! ctx)
      (is (= 1 (count (:games @ctx))))
      (is (s/valid? ::manager/context @ctx))))

  (testing "remove-game!"
    (let [ctx (make-context)
          game (create-game! ctx)]
      (remove-game! ctx (:id game))
      (is (empty? (:games ctx)))
      (is (s/valid? ::manager/context @ctx))))

  (testing "get-game-by-id"
    (let [ctx (make-context)
          game0 (create-game! ctx)
          game1 (get-game-by-id ctx (:id game0))]
      (is (= game0 game1))
      (is (s/valid? ::game/game game1))
      (is (s/valid? ::manager/context @ctx))))

  (testing "join-game!"
    (let [ctx (make-context)
          game (create-game! ctx)
          user (create-user! ctx "id0")]
      (join-game! ctx (:id game) (:id user))
      (is (= (:id game) (:game-id (get-user-by-id ctx (:id user)))))
      (is (some #{(:id user)} (-> (get-game-by-id ctx (:id game))
                                  :players
                                  keys)))
      (is (s/valid? ::manager/context @ctx))))

  (testing "join-new-game!"
    (let [ctx (make-context)
          user-id (:id (create-user! ctx "id0"))
          game (join-new-game! ctx user-id)]
      (is (= "id0" (-> game :players keys first)))
      (is (= (:id game) (-> @ctx :games keys first)))
      (is (= (:id game) (:game-id (get-user-by-id ctx user-id))))
      (is (s/valid? ::manager/context @ctx))))

  (testing "get-player-ids"
    (let [ctx (make-context)
          game (create-game! ctx)
          game-id (:id game)
          user1-id (:id (create-user! ctx "id0"))
          user2-id (:id (create-user! ctx "id1"))]
      (join-game! ctx game-id user1-id)
      (join-game! ctx game-id user2-id)
      (is (= (get-player-ids ctx game-id)
             ["id0" "id1"]))
      (is (s/valid? ::manager/context @ctx))))

  (testing "has-active-players?"
    (let [ctx (make-context)
          game (create-game! ctx)
          game-id (:id game)
          user-id "id0"]
      (create-user! ctx "id0")
      (is (false? (has-active-players? ctx game-id)))
      (join-game! ctx game-id user-id)
      (is (= "id0" (:id (get-user-by-id ctx "id0"))))
      (is (true? (has-active-players? ctx game-id)))
      (remove-user! ctx user-id)
      (is (empty? (:users @ctx)))
      (is (false? (has-active-players? ctx game-id)))
      (is (s/valid? ::manager/context @ctx))))
  )
