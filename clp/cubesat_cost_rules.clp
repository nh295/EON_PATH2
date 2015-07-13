(defrule CUBESAT-COST::compute-payload-volume
?miss <- (MANIFEST::Mission (instruments $?ins) (payload-volume# nil) (Name ?sat))
?c <- (accumulate (bind ?nunits 0.0)                        ;; initializer
                (bind ?nunits (+ ?nunits (/ (* ?x ?y ?z) 0.001)))                    ;; action
                ?nunits                                        ;; result
                (CAPABILITIES::Manifested-instrument (Name ?ins1&:(contains$ $?ins ?ins1)) (flies-in ?sat) (dimension-x# ?x&~nil) (dimension-y# ?y&~nil) (dimension-z# ?z&~nil))
				) ;; CE
	=>
(modify ?miss (payload-volume# ?c))
)


(defrule CUBESAT-COST::assign-bus
?miss <- (MANIFEST::Mission (payload-volume# ?vol&~nil) (bus nil))
=>
(modify ?miss (bus (assign-bus ?vol)))
)

(deffunction assign-bus (?vol)
(if(<= ?vol 1.0) then (return 3U))
(if(<= ?vol 2.5) then (return 6U))
(if(<= ?vol 5) then (return 12U))
(return 100U)
)

(defrule CUBESAT-COST::compute-lifetime
?miss <- (MANIFEST::Mission (lifetime nil) (orbit-altitude# ?h&~nil) (bus ?nunits&~nil))
=>
(modify ?miss (lifetime (assign-lifetime ?nunits ?h)))
)

(deffunction assign-lifetime (?nunits ?h)
(return (min (get-orbit-lifetime ?h) (get-bus-lifetime ?nunits)))
)

(deffunction get-orbit-lifetime (?h)
(if(<= ?h 450) then (return 0.5))
(if(<= ?h 500) then (return 1.0))
(if(<= ?h 600) then (return 5.0))
(return 25.0)
)

(deffunction get-bus-lifetime (?nunits)
(if(eq ?nunits 3U) then (return 1.0))
(if(eq ?nunits 6U) then (return 3.0))
(if(eq ?nunits 12U) then (return 3.0))
(if(eq ?nunits 100U) then (return 10.0))
(throw new Exception "CUBESAT-COST: unknown bus size in get-bus-lifetime")
)

(defrule CUBESAT-COST::replenishment-factor
?miss <- (MANIFEST::Mission (lifetime ?life&~nil) (replenishment-factor# nil) (time-horizon# ?th&~nil))
=>
(modify ?miss (replenishment-factor# (Math.ceil (/ ?th ?life))))
)

(defrule CUBESAT-COST::payload-cost
?miss <- (MANIFEST::Mission (payload-cost# nil) (instruments $?ins) (Name ?sat) (replenishment-factor# ?rf&~nil) (num-of-sats-per-plane# ?ns&~nil) (num-of-planes# ?np&~nil))
?c <- (accumulate (bind ?cost 0.0)                        ;; initializer
                (bind ?cost (+ ?cost ?ins-cost))                    ;; action
                ?cost                                        ;; result
                (CAPABILITIES::Manifested-instrument (Name ?ins1&:(contains$ $?ins ?ins1)) (flies-in ?sat) (cost# ?ins-cost))
				) ;; CE
=>
(modify ?miss (payload-cost# (* ?rf ?c ?np ?ns)))
)

(defrule CUBESAT-COST::bus-cost
?miss <- (MANIFEST::Mission (bus-cost# nil) (bus ?b&~nil) (replenishment-factor# ?rf&~nil) (num-of-sats-per-plane# ?ns&~nil) (num-of-planes# ?np&~nil))
=>
(modify ?miss (bus-cost# (* ?rf ?ns ?np (get-bus-cost ?b))))
)

(deffunction get-bus-cost(?nunits)
(if(eq ?nunits 3U) then (return 1.0))
(if(eq ?nunits 6U) then (return 3.0))
(if(eq ?nunits 12U) then (return 4.0))
(return 100.0)
)

(defrule CUBESAT-COST::launch-cost
?miss <- (MANIFEST::Mission (launch-cost# nil) (bus ?b&~nil) (orbit-string ?orb&~nil) (replenishment-factor# ?rf&~nil) (num-of-sats-per-plane# ?ns&~nil))
=>
(modify ?miss (launch-cost# (* ?rf (get-launch-cost ?b ?orb ?ns))))
)

(deffunction get-launch-cost(?nunits ?orb ?ns)
(if(eq ?orb GEO-35788-equat-NA) then (return 100.0))
(if(eq ?orb LEO-400-ISS-NA) then (return 5.0))
(if(eq ?orb SSO-600-SSO-DD) then (return 5.0))
(if(eq ?orb SSO-800-SSO-PM) then (return 5.0))
(if(eq ?orb SSO-800-SSO-AM) then (return 5.0))
(if(eq ?orb LEO-600-ISS-NA) then (return 5.0))
(throw new Exception "CUBESAT-COST: unknown orbit in get-launch-cost")
)

(defrule CUBESAT-COST::compute-mission-cost
?miss <- (MANIFEST::Mission (mission-cost# nil) (payload-cost# ?pc&~nil) (bus-cost# ?bc&~nil) (launch-cost# ?lc&~nil))
=>
(modify ?miss (mission-cost# (+ ?pc ?bc ?lc)))
)
