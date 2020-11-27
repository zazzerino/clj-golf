(ns golf.manager-test
  (:require [clojure.test :refer :all]
            [clojure.spec.alpha :as s]
            [golf.manager :refer :all :as manager]))

(deftest test-manager
  (testing "add users"
    (let [ctx (make-context)]
      (is (s/valid? ::manager/context @ctx))
      (add-user ctx "id0")
      (is (= 1 (count (:users @ctx))))
      (is (= "anon" (:name (get-user-by-id ctx "id0"))))))

  #_(testing "manage users"
    (let [ctx (make-context)
          user (make-user "chan0")]
      (is (empty? (:users ctx)))
      (is (empty? (:games ctx)))
      (add-user! ctx user)
      (is (= 1 (count (:users @ctx))))
      (is (= user (get-user-by-id ctx (:id user))))
      (is (= user (get-user-by-channel ctx (:channel user))))
      (login! ctx (:id user) "Bob")
      (is (= "Bob" (-> (get-user-by-id ctx (:id user)) :name)))
      (logout! ctx (:id user))
      (is (= "anon" (-> (get-user-by-id ctx (:id user)) :name)))
      (remove-user! ctx (:id user))
      (is (empty? (:users @ctx)))))
  )
