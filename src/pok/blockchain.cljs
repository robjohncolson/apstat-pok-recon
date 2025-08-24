(ns pok.blockchain
  "Transaction and Blockchain Module for AP Statistics PoK Blockchain
   Ports Racket transaction.rkt to ClojureScript with immutable structures
   Handles transaction creation, validation, and block mining"
  (:require [re-frame.core :as rf]
            [cljs.test :refer-macros [deftest is testing run-tests]]
            [clojure.string :as str]
            [pok.state :as state]))

;; Transaction record for PoK submissions
(defrecord Transaction [id timestamp pubkey question-id answer hash])

;; Block record for blockchain
(defrecord Block [hash transactions proposer timestamp difficulty])

;; Generate unique transaction ID
;; Input: pubkey, question-id, timestamp
;; Output: unique string ID
(defn generate-transaction-id 
  "Creates unique transaction ID from components"
  [pubkey question-id timestamp]
  (let [input (str pubkey question-id timestamp)]
    ;; Use first 16 chars of simple hash for now (would use crypto-js in production)
    (subs (.toString (hash input) 16) 0 16)))

;; Create transaction hash from content using browser crypto
;; Input: transaction record
;; Output: SHA-256 hash string (async)
(defn hash-transaction-async 
  "Hashes transaction content using Web Crypto API"
  [txn callback]
  (let [encoder (js/TextEncoder.)
        content (str (:id txn)
                    (:timestamp txn)
                    (:pubkey txn)
                    (:question-id txn)
                    (:answer txn))
        data (.encode encoder content)]
    (-> (.digest js/crypto.subtle "SHA-256" data)
        (.then (fn [hash-buffer]
                 (let [hash-array (js/Uint8Array. hash-buffer)
                       hex-string (->> hash-array
                                      (map #(.slice (str "0" (.toString % 16)) -2))
                                      (apply str))]
                   (callback hex-string)))))))

;; Synchronous hash for immediate validation (simplified)
(defn hash-transaction-sync 
  "Simple synchronous hash for validation"
  [txn]
  (let [content (str (:id txn) (:timestamp txn) (:pubkey txn) (:question-id txn) (:answer txn))]
    (.toString (hash content) 16)))

;; Create new transaction
;; Input: pubkey, question-id, answer
;; Output: complete transaction record with hash
(defn make-transaction 
  "Creates new PoK transaction with validation"
  [pubkey question-id answer]
  (let [timestamp (.now js/Date)
        id (generate-transaction-id pubkey question-id timestamp)
        txn (map->Transaction {:id id
                               :timestamp timestamp
                               :pubkey pubkey
                               :question-id question-id
                               :answer answer
                               :hash ""})]
    (assoc txn :hash (hash-transaction-sync txn))))

;; Transaction validation
(defn validate-transaction 
  "Validates transaction structure and content"
  [txn]
  (and (string? (:id txn))
       (> (count (:id txn)) 0)
       (number? (:timestamp txn))
       (> (:timestamp txn) 0)
       (state/valid-pubkey? (:pubkey txn))
       (string? (:question-id txn))
       (re-matches #"^U[0-9]+-L[0-9]+-Q[0-9]+$" (:question-id txn))
       (string? (:answer txn))
       (> (count (:answer txn)) 0)
       (= (:hash txn) (hash-transaction-sync txn))))

;; Block creation and hashing
(defn hash-transactions 
  "Hashes list of transactions for block header"
  [txns]
  (let [txn-hashes (map :hash txns)]
    (.toString (hash (str/join "" txn-hashes)) 16)))

(defn hash-block 
  "Creates block hash from header content"
  [blk]
  (let [transactions-hash (hash-transactions (:transactions blk))
        content (str transactions-hash
                    (:proposer blk)
                    (:timestamp blk)
                    (:difficulty blk))]
    (.toString (hash content) 16)))

;; Create new block
;; Input: list of transactions, proposer pubkey, difficulty
;; Output: complete block record with hash
(defn make-block 
  "Creates new block from transactions"
  [transactions proposer difficulty]
  (let [timestamp (.now js/Date)
        blk (map->Block {:hash ""
                         :transactions transactions
                         :proposer proposer
                         :timestamp timestamp
                         :difficulty difficulty})]
    (assoc blk :hash (hash-block blk))))

;; Block validation
(defn validate-block 
  "Validates complete block structure"
  [blk]
  (and (string? (:hash blk))
       (vector? (:transactions blk))
       (every? validate-transaction (:transactions blk))
       (state/valid-pubkey? (:proposer blk))
       (number? (:timestamp blk))
       (number? (:difficulty blk))
       (> (:difficulty blk) 0)
       (= (:hash blk) (hash-block blk))))

;; PoK-specific validations
(defn validate-answer-format 
  "Validates answer format by question type"
  [answer question-type]
  (case question-type
    "multiple-choice" (re-matches #"^[A-E]$" answer)
    "free-response" (and (string? answer) (> (count answer) 10))
    "simulation" (and (string? answer) (> (count answer) 5))
    false))

;; Transaction difficulty calculation
(defn calculate-transaction-difficulty 
  "Calculates difficulty based on question complexity"
  [question-id attachments]
  (let [base-difficulty 1
        has-chart (contains? attachments :chart-type)
        has-table (and (contains? attachments :table)
                       (> (count (:table attachments)) 0))]
    (+ base-difficulty
       (if has-chart 1 0)
       (if has-table 1 0))))

;; Re-frame events for blockchain operations

;; Submit transaction to mempool
(rf/reg-event-db
 :submit-transaction
 (fn [db [_ question-id answer]]
   (if-let [pubkey (get-in db [:profile :pubkey])]
     (let [txn (make-transaction pubkey question-id answer)]
       (if (validate-transaction txn)
         (update-in db [:blockchain :transactions] conj txn)
         db))
     db)))

;; Mine block from current transactions
(rf/reg-event-db
 :mine-block
 (fn [db [_ difficulty]]
   (let [transactions (get-in db [:blockchain :transactions])
         proposer (get-in db [:profile :pubkey])]
     (if (and (seq transactions) proposer)
       (let [block (make-block transactions proposer difficulty)]
         (-> db
             (update-in [:blockchain :blocks] conj block)
             (assoc-in [:blockchain :transactions] [])
             (assoc-in [:blockchain :current-block] block)))
       db))))

;; Validate incoming block
(rf/reg-event-db
 :validate-block
 (fn [db [_ block]]
   (if (validate-block block)
     (update-in db [:blockchain :blocks] conj block)
     db)))

;; Re-frame subscriptions for blockchain data

;; Get current transaction mempool
(rf/reg-sub
 :transaction-mempool
 (fn [db _]
   (get-in db [:blockchain :transactions])))

;; Get blockchain height
(rf/reg-sub
 :blockchain-height
 (fn [db _]
   (count (get-in db [:blockchain :blocks]))))

;; Get latest block
(rf/reg-sub
 :latest-block
 (fn [db _]
   (last (get-in db [:blockchain :blocks]))))

;; Mempool management
(defn add-to-mempool 
  "Adds validated transaction to mempool"
  [txn mempool]
  (if (validate-transaction txn)
    (conj mempool txn)
    mempool))

(defn remove-from-mempool 
  "Removes transactions by ID from mempool"
  [txn-ids mempool]
  (remove #(contains? (set txn-ids) (:id %)) mempool))

;; Block reward calculation
(defn calculate-block-reward 
  "Calculates reward for block proposer"
  [transactions]
  (let [base-reward 10
        txn-count (count transactions)]
    (+ base-reward (* txn-count 2))))

;; Utility functions
(defn transactions-by-pubkey 
  "Filters transactions by pubkey"
  [txns pubkey]
  (filter #(= (:pubkey %) pubkey) txns))

(defn transactions-by-question 
  "Filters transactions by question ID"
  [txns question-id]
  (filter #(= (:question-id %) question-id) txns))

(defn transaction-size-bytes 
  "Estimates transaction size in bytes"
  [txn]
  (count (str (:id txn) (:timestamp txn) (:pubkey txn) 
             (:question-id txn) (:answer txn) (:hash txn))))

;; Blockchain analysis functions
(defn get-user-transactions 
  "Gets all transactions for a user across blocks"
  [blocks pubkey]
  (->> blocks
       (mapcat :transactions)
       (filter #(= (:pubkey %) pubkey))))

(defn calculate-user-accuracy 
  "Calculates user accuracy from transaction history"
  [user-transactions correct-answers]
  (if (empty? user-transactions)
    0.0
    (let [total (count user-transactions)
          correct (count (filter #(contains? correct-answers (:question-id %)) user-transactions))]
      (/ correct total))))

;; Unit tests for blockchain operations
(deftest test-transaction-creation
  (let [test-pubkey "a1b2c3d4e5f6789012345678901234567890abcdef1234567890abcdef123456"
        test-txn (make-transaction test-pubkey "U1-L2-Q01" "B")]
    
    (testing "Transaction creation"
      (is (validate-transaction test-txn))
      (is (= (:pubkey test-txn) test-pubkey))
      (is (= (:question-id test-txn) "U1-L2-Q01"))
      (is (= (:answer test-txn) "B"))
      (is (string? (:hash test-txn)))
      (is (= (count (:hash test-txn)) 8))) ; Simplified hash length
    
    (testing "Transaction hashing consistency"
      (let [hash1 (hash-transaction-sync test-txn)
            hash2 (hash-transaction-sync test-txn)]
        (is (= hash1 hash2))))
    
    (testing "Different transactions have different hashes"
      (let [test-txn2 (make-transaction test-pubkey "U1-L2-Q02" "A")]
        (is (not= (:hash test-txn) (:hash test-txn2)))))))

(deftest test-block-creation
  (let [test-pubkey "a1b2c3d4e5f6789012345678901234567890abcdef1234567890abcdef123456"
        test-txn1 (make-transaction test-pubkey "U1-L2-Q01" "B")
        test-txn2 (make-transaction test-pubkey "U1-L2-Q02" "A")
        test-block (make-block [test-txn1 test-txn2] test-pubkey 1)]
    
    (testing "Block creation"
      (is (validate-block test-block))
      (is (= (count (:transactions test-block)) 2))
      (is (= (:proposer test-block) test-pubkey))
      (is (= (:difficulty test-block) 1)))
    
    (testing "Block hashing"
      (let [hash1 (hash-block test-block)
            hash2 (hash-block test-block)]
        (is (= hash1 hash2))
        (is (string? hash1))))))

(deftest test-validation
  (testing "Answer format validation"
    (is (validate-answer-format "B" "multiple-choice"))
    (is (not (validate-answer-format "X" "multiple-choice")))
    (is (validate-answer-format "This is a detailed explanation" "free-response"))
    (is (not (validate-answer-format "short" "free-response"))))
  
  (testing "Transaction difficulty calculation"
    (let [simple-attachments {}
          complex-attachments {:chart-type "bar" :table [["A" "B"]]}]
      (is (= (calculate-transaction-difficulty "U1-L1-Q01" simple-attachments) 1))
      (is (= (calculate-transaction-difficulty "U1-L1-Q01" complex-attachments) 3)))))

(deftest test-utility-functions
  (let [test-pubkey "a1b2c3d4e5f6789012345678901234567890abcdef1234567890abcdef123456"
        txn1 (make-transaction test-pubkey "U1-L2-Q01" "B")
        txn2 (make-transaction test-pubkey "U1-L2-Q02" "A")
        txns [txn1 txn2]]
    
    (testing "Transaction filtering"
      (is (= (count (transactions-by-pubkey txns test-pubkey)) 2))
      (is (= (count (transactions-by-question txns "U1-L2-Q01")) 1)))
    
    (testing "Transaction size calculation"
      (is (> (transaction-size-bytes txn1) 100)))
    
    (testing "Mempool operations"
      (let [mempool []
            mempool-with-txn (add-to-mempool txn1 mempool)]
        (is (= (count mempool-with-txn) 1))
        (let [mempool-removed (remove-from-mempool [(:id txn1)] mempool-with-txn)]
          (is (= (count mempool-removed) 0)))))))

;; Export for use in other namespaces
(def blockchain-namespace
  {:make-transaction make-transaction
   :validate-transaction validate-transaction
   :make-block make-block
   :validate-block validate-block
   :calculate-transaction-difficulty calculate-transaction-difficulty
   :transactions-by-pubkey transactions-by-pubkey
   :calculate-block-reward calculate-block-reward})