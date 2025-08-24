(ns pok.reputation
  "Reputation and Peer Attestation System for PoK Blockchain
   Phase 5 implementation with replay caps and fork decay tuning"
  (:require [cljs.test :refer-macros [deftest is testing]]))

;; Phase 5 Optimization: Replay cap to prevent spam
(def ^:const REPLAY-CAP 50)
(def ^:const DECAY-RATE 0.05) ; 5% decay per 24h window
(def ^:const MIN-QUORUM-SIZE 3)
(def ^:const CONSENSUS-THRESHOLD 0.67)
(def ^:const MINORITY-BONUS 1.5)

;; Reputation calculation with time decay and replay caps
(defn calculate-reputation
  "Calculates reputation with time decay and replay protection"
  [current-reputation accuracy attestations time-windows & {:keys [replay-cap] :or {replay-cap REPLAY-CAP}}]
  (let [;; Apply time decay (5% per 24h window)
        decayed-rep (* current-reputation (Math/pow (- 1 DECAY-RATE) time-windows))
        
        ;; Cap attestations to prevent replay attacks
        capped-attestations (take replay-cap attestations)
        
        ;; Calculate accuracy bonus
        accuracy-bonus (* accuracy 50)
        
        ;; Calculate attestation bonus
        attestation-bonus (* (count capped-attestations) 5)
        
        ;; Calculate final reputation
        new-reputation (+ decayed-rep accuracy-bonus attestation-bonus)]
    
    ;; Ensure reputation doesn't go below 0
    (max 0.0 new-reputation)))

;; Create attestation with validation
(defn make-attestation
  "Creates a validated attestation record"
  [validator-pubkey question-id submitted-answer correct-answer confidence]
  (when (and (string? validator-pubkey)
             (string? question-id)
             (string? submitted-answer)
             (string? correct-answer)
             (number? confidence)
             (>= confidence 0.0)
             (<= confidence 1.0))
    {:validator validator-pubkey
     :question-id question-id
     :submitted-answer submitted-answer
     :correct-answer correct-answer
     :confidence confidence
     :timestamp (.getTime (js/Date.))
     :hash (str (.toString (js/Math.random) 36) (.getTime (js/Date.)))}))

;; Form attestation quorum with minimum reputation requirement
(defn form-attestation-quorum
  "Forms attestation quorum with minimum reputation filter"
  [question-id validators min-reputation]
  (let [qualified-validators (filter #(>= (:reputation %) min-reputation) validators)
        selected-validators (take MIN-QUORUM-SIZE qualified-validators)]
    (when (>= (count selected-validators) MIN-QUORUM-SIZE)
      {:question-id question-id
       :validators selected-validators
       :formed-at (.getTime (js/Date.))
       :min-reputation min-reputation})))

;; Validate quorum consensus with threshold
(defn validate-quorum-consensus
  "Validates that quorum reaches consensus threshold"
  [attestations threshold]
  (when (>= (count attestations) MIN-QUORUM-SIZE)
    (let [total-weight (reduce + (map :confidence attestations))
          agreeing-weight (reduce + 
                                 (map :confidence 
                                      (filter #(= (:submitted-answer %) (:correct-answer %)) attestations)))
          consensus-ratio (/ agreeing-weight total-weight)]
      (>= consensus-ratio threshold))))

;; Calculate minority-correct bonus
(defn calculate-minority-bonus
  "Calculates bonus for minority-correct answers"
  [answer all-answers]
  (let [total-responses (count all-answers)
        same-answers (count (filter #(= % answer) all-answers))
        minority-ratio (/ same-answers total-responses)]
    (if (< minority-ratio 0.5)
      MINORITY-BONUS
      1.0)))

;; Reputation leaderboard with sorting
(defn create-reputation-leaderboard
  "Creates sorted reputation leaderboard"
  [profiles]
  (->> profiles
       (map (fn [profile]
              {:username (:username profile)
               :archetype (:archetype profile)
               :reputation (:reputation-score profile)
               :rank 0})) ; Rank will be added after sorting
       (sort-by :reputation >)
       (map-indexed (fn [idx profile] (assoc profile :rank (inc idx))))
       (take 10))) ; Top 10 leaderboard

;; Streak bonus calculation
(defn calculate-streak-bonus
  "Calculates bonus for consecutive correct answers"
  [streak-count]
  (cond
    (>= streak-count 10) 2.0
    (>= streak-count 5) 1.5
    (>= streak-count 3) 1.2
    :else 1.0))

;; Fork decay tuning for consensus
(defn calculate-fork-decay
  "Calculates reputation decay based on fork participation"
  [participation-ratio fork-duration-hours]
  (let [base-decay 0.02 ; 2% base decay
        duration-factor (/ fork-duration-hours 24) ; Normalize to days
        participation-factor (- 1 participation-ratio)] ; Higher participation = less decay
    (* base-decay duration-factor participation-factor)))

;; Attestation validation with comprehensive checks
(defn validate-attestation
  "Validates attestation structure and content"
  [attestation]
  (and (map? attestation)
       (string? (:validator attestation))
       (string? (:question-id attestation))
       (string? (:submitted-answer attestation))
       (string? (:correct-answer attestation))
       (number? (:confidence attestation))
       (>= (:confidence attestation) 0.0)
       (<= (:confidence attestation) 1.0)
       (number? (:timestamp attestation))
       (string? (:hash attestation))))

;; Phase 5 performance optimization for reputation updates
(defn batch-update-reputation
  "Efficiently batch update multiple reputation scores"
  [reputation-updates]
  (mapv (fn [{:keys [profile accuracy attestations time-windows]}]
          (assoc profile :reputation-score
                 (calculate-reputation (:reputation-score profile) 
                                     accuracy attestations time-windows)))
        reputation-updates))