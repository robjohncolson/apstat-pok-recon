(ns pok.flow
  "Complete PoK workflow orchestration and end-to-end processing
   Phase 4 implementation with full cycle testing and validation"
  (:require [pok.curriculum :as curriculum]
            [pok.state :as state]
            [pok.blockchain :as blockchain]
            [pok.reputation :as reputation]
            [pok.qr :as qr]
            [cljs.test :refer-macros [deftest is testing]]))

;; Phase 5 Testing: Complete 5-question cycle with reputation progression
(defn test-complete-cycle
  "Tests complete 5-question cycle with reputation progression"
  []
  (let [start-profile (state/map->Profile {:username "cycle-test" 
                                          :archetype :explorers 
                                          :pubkey "cyclekey123" 
                                          :reputation-score 100.0})
        questions ["U1-L1-Q01" "U1-L1-Q02" "U1-L2-Q01" "U1-L2-Q02" "U1-L3-Q01"]
        answers ["A" "B" "A" "C" "B"]
        correct-answers ["A" "B" "A" "C" "A"] ; Last one wrong for testing
        start-time (.now js/performance)]
    
    ;; Process each question-answer pair
    (loop [q-idx 0
           current-rep 100.0
           streak 0
           total-accuracy 0
           performance-times []]
      (if (>= q-idx (count questions))
        ;; Return final results
        {:questions-completed (count questions)
         :total-accuracy total-accuracy
         :final-streak streak
         :final-reputation current-rep
         :final-archetype (state/calculate-archetype (/ total-accuracy (count questions)) 
                                                     (/ (reduce + performance-times) (count performance-times))
                                                     (count questions) 0.6)
         :all-under-50ms (every? #(< % 50) performance-times)
         :average-performance (/ (reduce + performance-times) (count performance-times))}
        ;; Process next question
        (let [question-id (nth questions q-idx)
              answer (nth answers q-idx)
              correct (nth correct-answers q-idx)
              op-start (.now js/performance)
              
              ;; Simulate answer processing
              result (process-answer-submission question-id answer start-profile)
              op-end (.now js/performance)
              op-time (- op-end op-start)
              
              ;; Update metrics
              is-correct (= answer correct)
              new-accuracy (if is-correct (inc total-accuracy) total-accuracy)
              new-streak (if is-correct (inc streak) 0)
              new-rep (if is-correct (+ current-rep (* 10 (inc new-streak))) current-rep)]
          
          (recur (inc q-idx) new-rep new-streak new-accuracy (conj performance-times op-time)))))))

;; Mock answer processing for testing
(defn process-answer-submission
  "Processes answer submission with mock attestation and consensus"
  [question-id answer profile]
  (let [txn (blockchain/make-transaction (:pubkey profile) question-id answer)
        attestations (generate-mock-attestations question-id answer)
        consensus (validate-quorum-consensus attestations 0.67)]
    {:transaction txn
     :attestations attestations
     :consensus-reached consensus
     :reputation-delta (if consensus 10 0)}))

;; Generate mock peer attestations for testing
(defn generate-mock-attestations
  "Generates mock peer attestations for consensus testing"
  [question-id submitted-answer]
  (let [validators ["validator1" "validator2" "validator3" "validator4"]
        confidence-scores [0.8 0.9 0.7 0.85]]
    (mapv (fn [validator confidence]
            {:validator validator
             :question-id question-id
             :submitted-answer submitted-answer
             :correct-answer submitted-answer ; Mock consensus
             :confidence confidence
             :timestamp (.getTime (js/Date.))})
          validators confidence-scores)))

;; Validate quorum consensus
(defn validate-quorum-consensus
  "Validates that quorum reaches consensus threshold"
  [attestations threshold]
  (let [total-attestations (count attestations)
        agreeing-attestations (count (filter #(= (:submitted-answer %) (:correct-answer %)) attestations))
        consensus-ratio (/ agreeing-attestations total-attestations)]
    (and (>= total-attestations 3)
         (>= consensus-ratio threshold))))

;; Validate individual attestation
(defn validate-attestation
  "Validates individual attestation structure"
  [attestation]
  (and (contains? attestation :validator)
       (contains? attestation :question-id)
       (contains? attestation :submitted-answer)
       (contains? attestation :correct-answer)
       (contains? attestation :confidence)
       (contains? attestation :timestamp)
       (>= (:confidence attestation) 0.0)
       (<= (:confidence attestation) 1.0)))