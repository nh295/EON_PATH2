(clear)
(defmodule TEST)
(deftemplate TEST::PERMUTING-ARCH (multislot sequence) (multislot ordering) (multislot improve) (slot str))
(deftemplate TEST::BEFORE-CONSTRAINT (slot element) (multislot before))
(deftemplate TEST::AFTER-CONSTRAINT (slot element) (multislot after))
(deftemplate TEST::BETWEEN-CONSTRAINT (slot element) (multislot between))
(deftemplate TEST::NOT-BETWEEN-CONSTRAINT (slot element) (multislot not-between))

(deftemplate TEST::BEFORE-DATE-CONSTRAINT (slot element) (slot before))
(deftemplate TEST::AFTER-DATE-CONSTRAINT (slot element) (multislot after))
(deftemplate TEST::BETWEEN-DATE-CONSTRAINT (slot element) (multislot between))
(deftemplate TEST::NOT-BETWEEN-DATE-CONSTRAINT (slot element) (multislot not-between))

(deftemplate TEST::CONTIGUITY-CONSTRAINT (multislot elements))
(deftemplate TEST::NON-CONTIGUITY-CONSTRAINT (multislot elements))
(deftemplate TEST::SUBSEQUENCE-CONSTRAINT (multislot subsequence))

(deftemplate TEST::BY-BEGINNING-CONSTRAINT (multislot elements))
(deftemplate TEST::BY-MIDDLE-CONSTRAINT (multislot elements))
(deftemplate TEST::BY-END-CONSTRAINT (multislot elements))

(deftemplate TEST::FIX-POSITION-CONSTRAINT (slot element) (slot position))


(deftemplate TEST::NEAR-CONSTRAINT (multislot elements))
(deftemplate TEST::FAR-CONSTRAINT (multislot elements))

(deftemplate TEST::BUDGET (multislot budgets) (multislot years))
(deftemplate TEST::COSTS (multislot elements) (multislot costs))

(defquery TEST::search-budget
    (TEST::BUDGET (budgets ?budgets) (years ?years))
    )

(defquery TEST::search-costs   
    (TEST::COSTS (elements ?elems) (costs ?costs))
    )

(deffunction get-budgets ()
    (bind ?result (run-query* TEST::search-budget))
    (?result ?next)
    (return (?result getSymbol budgets))  
    )

(deffunction get-years ()
    (bind ?result (run-query* TEST::search-budget))
    (?result ?next)
    (return (?result getSymbol years))  
    )

(deffunction get-costs ()
    (bind ?result (run-query* TEST::search-costs))
    (?result ?next)
    (return (?result getSymbol costs))  
    )

(deffunction rare-randb (?p)  
    (return (< (random) (* ?p 65536)))
    )

(defrule TEST::enumerate-some-permutations
    "This rule enumerates some random permutations of a set of m elements"
    ?arch <- (TEST::PERMUTING-ARCH (sequence $?seq))
    (test (< (length$ ?seq) 4))
    =>
    (retract ?arch)
    (bind ?n (length$ ?seq))
    (for (bind ?i 1) (<= ?i 4) (++ ?i) 
        (if (eq (member$ ?i ?seq) FALSE) then 
            (bind ?new-seq (insert$ ?seq (+ ?n 1) ?i))
        (if (rare-randb 1.0) then (assert (TEST::PERMUTING-ARCH (sequence ?new-seq) (improve yes))))
            )
        )
    )

(defquery TEST::count-EPP-architectures
    ?arch <- (TEST::PERMUTING-ARCH (sequence $?seq))
    )




;; ***************
;; with constraints
;; ***************

(deffunction sequence-to-ordering (?seq)
    (bind ?ord (create$ ))
    (foreach ?x ?seq (bind ?ord (insert$ ?ord (+ (length$ ?ord) 1) 0)))
    (for (bind ?i 1) (<= ?i (length$ ?seq)) (++ ?i)
        (bind ?j (nth$ ?i ?seq))
    (bind ?ord (replace$ ?ord ?j ?j ?i))
        )
    (return ?ord)
    )

(deffunction ordering-to-sequence(?ord)
    (bind ?seq (create$ ))
    (foreach ?x ?ord (bind ?seq (insert$ ?seq (+ (length$ ?seq) 1) 0)))
    (for (bind ?i 1) (<= ?i (length$ ?ord)) (++ ?i)
        (bind ?j (nth$ ?i ?ord))
    (bind ?seq (replace$ ?seq ?j ?j ?i))
        )
    (return ?seq)
    )


(deffunction check-precedence-in-ordering-binary (?el1 ?el2 ?ord)
    "returns TRUE if ?el1 goes before ?el2 and FALSE otherwise"
    (return (< (nth$ ?el1 ?ord) (nth$ ?el2 ?ord)))
    )

(deffunction check-precedence-in-sequence-binary (?el1 ?el2 ?seq)
    "returns TRUE if ?el1 goes before ?el2 and FALSE otherwise"
    (bind ?ord (sequence-to-ordering ?seq))
    (return (check-precedence-in-ordering-binary ?el1 ?el2 ?ord))
    )

(deffunction check-succession-in-ordering-binary (?el1 ?el2 ?ord)
    "returns TRUE if ?el1 goes after ?el2 and FALSE otherwise"
    (return (not (check-precedence-in-ordering-binary ?el1 ?el2 ?ord)))
    )

(deffunction check-succession-in-sequence-binary (?el1 ?el2 ?seq)
    "returns TRUE if ?el1 goes after ?el2 and FALSE otherwise"
    (return (not (check-precedence-in-sequence-binary ?el1 ?el2 ?seq)))
    )

(deffunction check-precedence-in-ordering (?el1 ?list ?ord)
    "returns TRUE if ?el1 goes before all elements in ?el2 and FALSE otherwise"
    (if (eq (listp ?list) FALSE) then 
        (return (check-precedence-in-ordering-binary ?el1 ?list ?ord)))
    (if (eq (length$ ?list) 1) then 
        (bind ?el2 (nth$ 1 ?list) ) 
        (return (check-precedence-in-ordering-binary ?el1 ?el2 ?ord))
        else 
        (bind ?el2 (nth$ 1 ?list))
        (bind ?res (check-precedence-in-ordering-binary ?el1 ?el2 ?ord))
        (if (eq ?res FALSE ) then (return FALSE) else 
            (return (check-precedence-in-ordering ?el1 (rest$ ?list) ?ord)))
        )
    )

(deffunction check-precedence-in-sequence (?el1 ?list ?seq)
    "returns TRUE if ?el1 goes before ?el2 and FALSE otherwise"
    (bind ?ord (sequence-to-ordering ?seq))
    (return (check-precedence-in-ordering  ?el1 ?list ?ord))
    )

(deffunction check-succession-in-ordering (?el1 ?list ?ord)
    "returns TRUE if ?el1 goes after all elements in ?el2 and FALSE otherwise"
    (if (eq (listp ?list) FALSE) then 
        (return (check-succession-in-ordering-binary ?el1 ?list ?ord)))
    (if (eq (length$ ?list) 1) then 
        (bind ?el2 (nth$ 1 ?list) ) 
        (return (check-succession-in-ordering-binary ?el1 ?el2 ?ord))
        else 
        (bind ?el2 (nth$ 1 ?list))
        (bind ?res (check-succession-in-ordering-binary ?el1 ?el2 ?ord))
        (if (eq ?res FALSE ) then (return FALSE) else 
            (return (check-succession-in-ordering ?el1 (rest$ ?list) ?ord)))
        )
    )

(deffunction check-succession-in-sequence (?el1 ?list ?seq)
    "returns TRUE if ?el1 goes after ?el2 and FALSE otherwise"
    (bind ?ord (sequence-to-ordering ?seq))
    (return (check-succession-in-ordering  ?el1 ?list ?ord))
    )


(defrule TEST::enforce-BEFORE-constraints
    "If an architecture does not does not satisfy a before constraint 
    then it should be deleted"
    
    ?arch <- (TEST::PERMUTING-ARCH (sequence $?seq))
    (TEST::BEFORE-CONSTRAINT (element ?el) (before $?elems))
    (test (eq (check-precedence-in-sequence ?el ?elems ?seq) FALSE))  
    =>     
    (retract ?arch)
    )

(defrule TEST::enforce-AFTER-constraints
    "If an architecture does not does not satisfy an AFTER constraint 
    then it should be deleted"
    
    ?arch <- (TEST::PERMUTING-ARCH (sequence $?seq))
    (TEST::AFTER-CONSTRAINT (element ?el) (after $?elems))
    (test (eq (check-succession-in-sequence ?el ?elems ?seq) FALSE))  
    =>    
    (retract ?arch)
    )

(defrule TEST::enforce-BETWEEN-constraints
    "If an architecture does not does not satisfy a BETWEEN constraint 
    then it should be deleted"
    
    ?arch <- (TEST::PERMUTING-ARCH (sequence $?seq))
    (TEST::BETWEEN-CONSTRAINT (element ?el) (between $?elems))
    (or (test (eq (check-succession-in-sequence ?el (first$ ?elems) ?seq) FALSE)) 
    (test (eq (check-precedence-in-sequence ?el (rest$ ?elems) ?seq) FALSE)) ) 
    =>    
    (retract ?arch)
    )

(defrule TEST::enforce-NOT-BETWEEN-constraints
    "If an architecture does not does not satisfy a BETWEEN constraint 
    then it should be deleted"
    
    ?arch <- (TEST::PERMUTING-ARCH (sequence $?seq))
    (TEST::NOT-BETWEEN-CONSTRAINT (element ?el) (not-between $?elems))
    (test (eq (check-precedence-in-sequence ?el (first$ ?elems) ?seq) FALSE)) 
    (test (eq (check-succession-in-sequence ?el (rest$ ?elems) ?seq) FALSE)) 
    =>    
    (retract ?arch)
    )

(deffunction min$ (?list)
    (return (eval (str-cat "(min " (implode$ ?list) ")" )))
    )

(deffunction sort$ (?list)
    (if (eq (length$ ?list) 1) then (return (nth$ 1 ?list)))
    (bind ?mn (min$ ?list))
	(bind ?index (member$ ?mn ?list))
    (bind ?shorter-list (replace$ ?list ?index ?index (create$ )))
    (return (create$ ?mn (sort$ ?shorter-list)))
    )

(deffunction check-contiguity-in-sequence (?elems ?seq)
    (if (eq (length$ ?elems) 1) then (return TRUE))
    (bind ?ord (sequence-to-ordering ?seq))
    ;(printout t ?ord crlf)
    (bind ?indexes (create$ ))
    (for (bind ?i 1) (<= ?i (length$ ?elems)) (++ ?i) 
        (bind ?indexes (insert$ ?indexes ?i (nth$ (nth$ ?i ?elems) ?ord)))
        )
    (bind ?indexes (sort$ ?indexes))
    (printout t ?indexes crlf)
    (for (bind ?i 1) (< ?i (length$ ?elems)) (++ ?i) 
        (if (neq (- (nth$ (+ ?i 1) ?indexes) (nth$ (+ ?i) ?indexes)) 1) then (return FALSE))
        )
    (return TRUE)
    )

(defrule TEST::enforce-CONTIGUITY-constraints
    "If an architecture does not does not satisfy a CONTIGUITY constraint 
    then it should be deleted"
    
    ?arch <- (TEST::PERMUTING-ARCH (sequence $?seq))
    (TEST::CONTIGUITY-CONSTRAINT (elements $?elems) )
    (test (eq (check-contiguity-in-sequence ?elems ?seq) FALSE)) 
    =>    
    (retract ?arch)
    )

(defrule TEST::enforce-NON-CONTIGUITY-constraints
    "If an architecture does not does not satisfy a NON CONTIGUITY constraint 
    then it should be deleted"
    
    ?arch <- (TEST::PERMUTING-ARCH (sequence $?seq))
    (TEST::NON-CONTIGUITY-CONSTRAINT (elements $?elems) )
    (test (eq (check-contiguity-in-sequence ?elems ?seq) TRUE)) 
    =>    
    (retract ?arch)
    )

(deffunction is-subsequence (?subseq ?seq)
	(return (numberp (str-index (implode$ ?subseq) (implode$ ?seq))))
    )

(defrule TEST::enforce-SUBSEQUENCE-constraints
    "If an architecture does not does not satisfy a SUBSEQUENCE constraint 
    then it should be deleted"
    
    ?arch <- (TEST::PERMUTING-ARCH (sequence $?seq))
    (TEST::SUBSEQUENCE-CONSTRAINT (subsequence $?elems) )
    (test (eq (is-subsequence ?elems ?seq) FALSE)) 
    =>    
    (retract ?arch)
    )

(deffunction get-sublist-with-indexes (?list ?indexes)
    (bind ?sub (create$ ))
    (foreach ?i ?indexes 
        (bind ?sub (insert$ ?sub (+ 1 (length$ ?sub)) (nth$ ?i ?list)))
    )
    (return ?sub)
    )

(deffunction EPP-crossover (?dad ?mom); dad = (1 4 5 2 3), mom = (2 4 5 1 3)
    (bind ?n (length$ ?dad))
    (bind ?m (round (/ ?n 2)))
    (bind ?child (subseq$ ?dad 1 ?m)); first half of sequence from dad: (1 4 5)
    (for (bind ?i 1) (<= ?i ?n) (++ ?i); second half from mom: (2 3)
    (bind ?el (nth$ ?i ?mom))
        (if (not (subsetp (create$ ?el) ?child)) then (bind ?child (insert$ ?child (+ (length$ ?child) 1) ?el))))
    (return ?child)
    )




(deffunction sequence-launch-dates (?seq)
    (bind ?budgets (get-budgets))
    (bind ?years (get-years))
    (bind ?costs (get-costs))
    (bind ?dates (create$ ))
    (bind ?start-date (nth$ 1 ?years))
    (bind ?date ?start-date)
    (for (bind ?i 1) (< ?i (length$ ?costs)) (++ ?i)
        (bind ?date (+ ?date (/ (nth$ ?i ?costs) (nth$ (round (- ?date ?start-date)) ?budgets))))
        (bind ?dates (insert$ ?dates ?i ?date))
        )
    (return ?dates)
    )

(deffunction by-beginning-binary (?elem ?seq)
    (if (<= (nth$ ?elem (sequence-to-ordering ?seq)) (round (/ (length$ ?seq) 3))) 
        then (return TRUE) else (return FALSE))
    )

(deffunction by-middle-binary (?elem ?seq)
    (if (and 
            (> (nth$ ?elem (sequence-to-ordering ?seq)) (round (/ (length$ ?seq) 3)))
            (<= (nth$ ?elem (sequence-to-ordering ?seq)) (round (* 2 (/ (length$ ?seq) 3))))
            ) 
        then (return TRUE) else (return FALSE))
    )

(deffunction by-end-binary (?elem ?seq)
    (if (> (nth$ ?elem (sequence-to-ordering ?seq)) (round (* 2 (/ (length$ ?seq) 3))))           
        then (return TRUE) else (return FALSE))
    )

(deffunction by-beginning (?elems ?seq)
    (if (not (listp ?elems)) then (return (by-beginning-binary ?elems ?seq)))   
    (if (eq (length$ ?elems) 1) then (return (by-beginning-binary (nth$ 1 ?elems) ?seq))
        else 
        (if (eq (by-beginning-binary (nth$ 1 ?elems) ?seq) FALSE) then (return FALSE)
            else (return (by-beginning  (rest$ ?elems) ?seq))))
    )

(deffunction by-middle (?elems ?seq)
    (if (not (listp ?elems)) then (return (by-middle-binary ?elems ?seq)))   
    (if (eq (length$ ?elems) 1) then (return (by-middle-binary (nth$ 1 ?elems) ?seq))
        else 
        (if (eq (by-middle-binary (nth$ 1 ?elems) ?seq) FALSE) then (return FALSE)
            else (return (by-middle  (rest$ ?elems) ?seq))))
    )

(deffunction by-end (?elems ?seq)
    (if (not (listp ?elems)) then (return (by-end-binary ?elems ?seq)))   
    (if (eq (length$ ?elems) 1) then (return (by-end-binary (nth$ 1 ?elems) ?seq))
        else 
        (if (eq (by-end-binary (nth$ 1 ?elems) ?seq) FALSE) then (return FALSE)
            else (return (by-end  (rest$ ?elems) ?seq))))
    )

(defrule TEST::enforce-by-beginning-constraint
    (TEST::BY-BEGINNING-CONSTRAINT (elements $?elems))
    ?arch <- (TEST::PERMUTING-ARCH (sequence $?seq))
    (test (eq (by-beginning ?elems ?seq) FALSE))
    =>
    (retract ?arch)
    )

(defrule TEST::enforce-by-middle-constraint
    (TEST::BY-MIDDLE-CONSTRAINT (elements $?elems))
    ?arch <- (TEST::PERMUTING-ARCH (sequence $?seq))
    (test (eq (by-middle ?elems ?seq) FALSE))
    =>
    (retract ?arch)
    )

(defrule TEST::enforce-by-end-constraint
    (TEST::BY-END-CONSTRAINT (elements $?elems))
    ?arch <- (TEST::PERMUTING-ARCH (sequence $?seq))
    (test (eq (by-end ?elems ?seq) FALSE))
    =>
    (retract ?arch)
    )

(deffunction is-in-position (?elem ?pos ?seq)
    (return (eq (nth$ ?elem (sequence-to-ordering ?seq)) ?pos))  
    )

(defrule TEST::enforce-fix-position-constraint
    (TEST::FIX-POSITION-CONSTRAINT (element ?elem) (position ?pos))
    ?arch <- (TEST::PERMUTING-ARCH (sequence $?seq))
    (test (eq (is-in-position ?elem ?pos ?seq) FALSE))
    =>
    (retract ?arch)
    )

(reset)
(assert (TEST::PERMUTING-ARCH (sequence (create$ ))))
(focus TEST)
(watch rules)
(run)
(bind ?n (count-query-results TEST::count-EPP-architectures))
(printout t "there are " ?n " architectures" crlf)

(assert (TEST::BEFORE-CONSTRAINT (element 3) (before 2 4)))
;(assert (TEST::AFTER-CONSTRAINT (element 3) (after 2 4)))
;(assert (TEST::BETWEEN-CONSTRAINT (element 3) (between 2 4)))
;(assert (TEST::NOT-BETWEEN-CONSTRAINT (element 3) (not-between 2 4)))
;(assert (TEST::CONTIGUITY-CONSTRAINT (elements 3 4)))
;(assert (TEST::NON-CONTIGUITY-CONSTRAINT (elements 3 4)));
;(assert (TEST::SUBSEQUENCE-CONSTRAINT (subsequence 1 2)))
(focus TEST)
(run)
(bind ?n (count-query-results TEST::count-EPP-architectures))
(printout t "there are " ?n " architectures" crlf)
(facts)
;(reset)
;(reset)
;(assert (TEST::PERMUTING-ARCH (sequence 1 2 3 4) (ordering ) (improve yes)))
;(assert (TEST::PERMUTING-ARCH (sequence 4 3 2 1) (ordering ) (improve yes)))
;(watch rules)
;(focus TEST)
;(run)
;(facts)