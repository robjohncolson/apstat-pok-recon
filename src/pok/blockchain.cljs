(ns pok.blockchain
  "PoK Transaction Schema and Blockchain Operations
   Phase 5 implementation with validation and mining simulation"
  (:require [cljs.test :refer-macros [deftest is testing]]))

;; Transaction record definition
(defrecord Transaction [id timestamp pubkey question-id answer hash])

;; Block record definition  
(defrecord Block [hash timestamp transactions proposer difficulty])

;; Transaction creation with validation
(defn make-transaction
  "Creates a validated PoK transaction"
  [pubkey question-id answer]
  (when (and (string? pubkey) (string? question-id) (string? answer))
    (let [timestamp (.getTime (js/Date.))
          id (str "tx_" (.toString (js/Math.random) 36) "_" timestamp)
          hash (str "hash_" (.toString (js/Math.random) 36))]
      (map->Transaction {:id id
                        :timestamp timestamp
                        :pubkey pubkey
                        :question-id question-id
                        :answer answer
                        :hash hash}))))

;; Block creation with mining simulation
(defn make-block
  "Creates a block from transactions with mining simulation"
  [transactions proposer difficulty]
  (when (and (vector? transactions) (string? proposer) (number? difficulty))
    (let [timestamp (.getTime (js/Date.))
          tx-hashes (mapv :hash transactions)
          block-hash (str "block_" (.toString (js/Math.random) 36) "_" timestamp)]
      (map->Block {:hash block-hash
                  :timestamp timestamp
                  :transactions transactions
                  :proposer proposer
                  :difficulty difficulty}))))

;; Transaction validation
(defn validate-transaction
  "Validates transaction structure and content"
  [transaction]
  (and (map? transaction)
       (string? (:id transaction))
       (number? (:timestamp transaction))
       (string? (:pubkey transaction))
       (string? (:question-id transaction))
       (string? (:answer transaction))
       (string? (:hash transaction))
       ;; Question ID format validation (U#-L#-Q##)
       (re-matches #"U\d+-L\d+-Q\d+" (:question-id transaction))
       ;; Answer format validation (A, B, C, D for multiple choice)
       (re-matches #"[A-D]|free-response|simulation" (:answer transaction))))

;; Block validation
(defn validate-block
  "Validates block structure and transactions"
  [block]
  (and (map? block)
       (string? (:hash block))
       (number? (:timestamp block))
       (vector? (:transactions block))
       (string? (:proposer block))
       (number? (:difficulty block))
       ;; Validate all transactions in block
       (every? validate-transaction (:transactions block))
       ;; Ensure at least one transaction
       (pos? (count (:transactions block)))
       ;; Difficulty range validation
       (>= (:difficulty block) 1)
       (<= (:difficulty block) 10)))

;; Answer format validation for different question types
(defn validate-answer-format
  "Validates answer format based on question type"
  [answer question-type]
  (case question-type
    "multiple-choice" (re-matches #"[A-D]" answer)
    "free-response" (and (string? answer) (>= (count answer) 1))
    "simulation" (and (string? answer) (or (= answer "run") (= answer "reset")))
    false))

;; Mempool management
(defn add-to-mempool
  "Adds validated transaction to mempool"
  [mempool transaction]
  (if (validate-transaction transaction)
    (conj mempool transaction)
    mempool))

(defn remove-from-mempool
  "Removes transactions that are included in a block"
  [mempool block-transactions]
  (let [included-ids (set (map :id block-transactions))]
    (filterv #(not (contains? included-ids (:id %))) mempool)))

;; Mining simulation
(defn mine-block-from-mempool
  "Mines a block from current mempool transactions"
  [mempool proposer difficulty max-transactions]
  (when (seq mempool)
    (let [selected-txns (take max-transactions mempool)
          new-block (make-block selected-txns proposer difficulty)]
      {:block new-block
       :updated-mempool (remove-from-mempool mempool selected-txns)})))

;; Blockchain validation
(defn validate-blockchain
  "Validates entire blockchain structure"
  [blockchain]
  (and (vector? blockchain)
       (every? validate-block blockchain)
       ;; Check timestamp ordering
       (or (empty? blockchain)
           (apply <= (map :timestamp blockchain)))))

;; Transaction fee calculation (simplified)
(defn calculate-transaction-fee
  "Calculates transaction fee based on complexity"
  [question-type answer-length]
  (case question-type
    "multiple-choice" 1
    "free-response" (max 2 (Math/ceil (/ answer-length 50)))
    "simulation" 3
    1))