;; **********************
;; SMAP EXAMPLE ENUMERATION RULES
;; ***************************

(deftemplate MANIFEST::ARCHITECTURE (slot bitString) (multislot payload) (slot num-sats-per-plane) (slot source) (slot orbit) 
    (slot orbit-altitude) (slot orbit-raan) (slot orbit-type) (slot orbit-inc) (slot num-planes)
    (multislot doesnt-fly) (slot num-sats-per-plane) (slot lifecycle-cost) (slot benefit)  
	(slot space-segment-cost) (slot ground-segment-cost) (slot pareto-ranking) (slot utility)
	(slot mutate) (slot crossover)  (slot improve) (slot id) 
    (slot num-instruments) (multislot sat-assignments) (multislot ground-stations) (multislot constellations))

(deftemplate DATABASE::list-of-instruments (multislot list))

(deffacts DATABASE::list-of-instruments (DATABASE::list-of-instruments 
        (list (create$ EON_BASE EON_50_1 EON_118_1 EON_183_1 EON_ATMS_1 PATH_GEOSTAR))))
(reset)
(defquery DATABASE::get-instruments 
    ?f <- (DATABASE::list-of-instruments (list $?l))
    )

(deffunction get-instruments ()
    (bind ?res (run-query* DATABASE::get-instruments))
    (?res next)
    (bind ?f (?res getObject f))
    (return ?f.list)
    )

(deffunction get-my-instruments ()
	(return (MatlabFunction getInstrumentList))
    )

(deffunction set-my-instruments (?list)
    ;(matlabf get_instrument_list ?list)
    ;(return TRUE)
    )


(deffunction create-index-of ()
    (bind ?prog "(deffunction index-of (?elem) ")
    (bind ?i 0)
    (bind ?smap-instruments (get-instruments))
    (foreach ?el ?smap-instruments
        (bind ?prog (str-cat ?prog " (if (eq (str-compare ?elem " ?el ") 0) then (return " (++ ?i) ")) "))
        )
    (bind ?prog (str-cat ?prog "(return -1))"))
    ;(printout t ?prog crlf)
    (build ?prog)
    )



(create-index-of)


(deffunction get-instrument (?ind)
    (return (nth$ ?ind (get-instruments)))
    )

(deffunction get-my-instrument (?ind)
    (return (eval (nth$ ?ind (get-my-instruments))))
    )

(defrule MANIFEST::ATMS-subinstruments
(CAPABILITIES::Manifested-instrument (Name EON_ATMS_1) (flies-in ?sat))
=>
(assert (CAPABILITIES::Manifested-instrument (Name EON_50_1) (flies-in ?sat) (Id ATMS_EON_50)))
(assert (CAPABILITIES::Manifested-instrument (Name EON_183_1) (flies-in ?sat) (Id ATMS_EON_183)))
(assert (SYNERGIES::cross-registered-instruments (platform ?sat) (degree-of-cross-registration spacecraft) (instruments (create$ EON_50_1 EON_183_1))))
)

;; **********************
;; SMAP EXAMPLE MANIFEST RULES
;; ***************************


(deffunction to-indexes (?instrs)
    (bind ?list (create$ ))   
    (for (bind ?i 1) (<= ?i (length$ ?instrs)) (++ ?i)
        (bind ?list (insert$ ?list ?i (my-index-of (nth$ ?i ?instrs))))
        )
    (return ?list)
    )

(deffunction to-strings (?indexes)

    (return (map get-my-instrument ?indexes))
    )

(deffunction pack-assignment-to-sats (?ass)
    (bind ?list (create$ )) (bind ?n 1)
    (for (bind ?i 1) (<= ?i (length$ ?ass)) (++ ?i)
        (bind ?indexes (find$ ?i ?ass))
        (if (isempty$ ?indexes) then (continue))
        (bind ?list (insert$ ?list ?n "sat")) (++ ?n)
        (bind ?sat-ins (to-strings ?indexes))
        (bind ?list (insert$ ?list ?n ?sat-ins)) (bind ?n (+ ?n (length$ ?sat-ins)))
        
        ) 
    (return ?list)   
    )



(deffunction pack-sats-to-assignment (?sats ?n)
    (bind ?nsat 0) (bind ?ass (create-list-n$ ?n))
    (for (bind ?i 1) (<= ?i (length$ ?sats)) (++ ?i)
        (bind ?el (nth$ ?i ?sats))
        ;(printout t ?el " eq sat? " (eq "sat" ?el) "  nsat " ?nsat crlf)
        (if (eq "sat" ?el) then (++ ?nsat) else 
            ;(printout t "ass " ?ass " element " ?el " index " (index-of ?el) " nsat " ?nsat crlf) 
            (bind ?ass (replace$ ?ass (my-index-of ?el) (my-index-of ?el) ?nsat))
            )
        )
    (return ?ass)
    )


;; **********************
;; SMAP EXAMPLE CAPABILITY RULES
;; ***************************
(deffunction contains$ (?list ?elem)
    (if (eq (length$ ?list) 0) then (return FALSE))
    (if (eq (first$ ?list) (create$ ?elem)) then (return TRUE) else
         (return (contains$ (rest$ ?list) ?elem)))    
    )

(defrule MANIFEST::EON-add-baseline-instrument
;;This is a constraint that only LEO instruments can be added to LEO orbits and GEO instruments can only be added to GEO
    (declare (salience 100))
    ?miss <- (MANIFEST::Mission (orbit-string ~GEO) (instruments $?list-of-instruments))
    (test (eq (contains$ ?list-of-instruments EON_BASE) FALSE))
       =>
    (modify ?miss (instruments (add-element$ ?list-of-instruments EON_BASE)))
    )
	
(deffunction compute-spatial-resolution-EON (?h ?f ?D)
    (return (* 2 1000 ?h (tan (to-deg (* 0.5 (/ (/ 3e8 ?f) ?D) )))))
    )

(deffunction compute-vertical-spatial-resolution-EON (?num)
    (return (* 4472.1 (exp (* -0.012 ?num))))
    )
	
(defrule MANIFEST::compute-EON-horizontal-spatial-resolution
    ?EON <- (CAPABILITIES::Manifested-instrument  (Name ?name&:(neq (str-index EON ?name) FALSE)) 
         (frequency# ?f&~nil) (orbit-altitude# ?h&~nil) (Horizontal-Spatial-Resolution# nil) (flies-in ?sat))
    (CAPABILITIES::Manifested-instrument  (Name EON_BASE) (Aperture-azimuth# ?D&~nil) (flies-in ?sat))
    =>
    (modify ?EON (Horizontal-Spatial-Resolution# (compute-spatial-resolution-EON ?h ?f ?D)))
    )



(deffunction between (?x ?mn ?mx)
    ;(printout t ?x " " ?mn " " ?mx crlf)
    ;(printout t ">= x min " (>= ?x ?mn)  " <= x max = " (<= ?x ?mx) crlf)
    (return 
        (and 
            (>= ?x ?mn) (<= ?x ?mx)))
    )



;; **********************
;; SMAP EXAMPLE EMERGENCE RULES
;; ***************************

;; **********************
;; SMAP VALUES BY DEFAULT
;; ***************************

(defrule MANIFEST::put-ADCS-values-by-default
"Use values  by default for satellite parameters"
?miss <- (MANIFEST::Mission  (ADCS-requirement nil))
=>
(modify ?miss (ADCS-requirement 0.01) (ADCS-type three-axis) (propellant-ADCS hydrazine)
 (propellant-injection hydrazine) (slew-angle 2.0)
)
)

(defrule SYNERGIES::pressure
?m <- (REQUIREMENTS::Measurement (Parameter "1.2.1 Atmospheric temperature") (Id ?id1) (taken-by ?ins1&~PATH_GEOSTAR) (orbit-string ?orb&~nil) (frequency# ?freq1))
(REQUIREMENTS::Measurement (Parameter "1.3.1 Atmospheric humidity") (Id ?id2) (taken-by ?ins2&~PATH_GEOSTAR) (frequency# ?freq2))
(SYNERGIES::cross-registered (measurements $?meas&:(contains$ $?meas ?id1)&:(contains$ $?meas ?id2)) (degree-of-cross-registration spacecraft))
=>
(duplicate ?m (Parameter "1.3.4 Atmospheric pressure") (Accuracy# 0.9) (Vertical-Spatial-Resolution# 2000) (Horizontal-Spatial-Resolution# 29480.12235534658) 
(Id (str-cat ?id1 "-syn-" ?id2)) (taken-by (str-cat ?ins1 "-syn-" ?ins2)) (Instrument (str-cat ?ins1 "-syn-" ?ins2)) (frequency# (str-cat ?freq1 "-syn-" ?freq2)))
)

(defrule SYNERGIES::pressure-at-surface
?m <- (REQUIREMENTS::Measurement (Parameter "1.2.2 Air temperature at surface") (Id ?id1) (taken-by ?ins1&~PATH_GEOSTAR) (orbit-string ?orb&~nil) (frequency# ?freq1))
(REQUIREMENTS::Measurement (Parameter "1.3.5 Air humidity at surface") (Id ?id2) (taken-by ?ins2&~PATH_GEOSTAR) (frequency# ?freq2))
(SYNERGIES::cross-registered (measurements $?meas&:(contains$ $?meas ?id1)&:(contains$ $?meas ?id2)) (degree-of-cross-registration spacecraft))
=>
(duplicate ?m (Parameter "1.3.6 Air pressure at surface") (Accuracy# 0.9) (Vertical-Spatial-Resolution# 2000) (Horizontal-Spatial-Resolution# 29480.12235534658)
(Id (str-cat ?id1 "-syn-" ?id2)) (taken-by (str-cat ?ins1 "-syn-" ?ins2)) (Instrument (str-cat ?ins1 "-syn-" ?ins2)) (frequency# (str-cat ?freq1 "-syn-" ?freq2)))
)

;; **********************
;; FOR ATMS ACCURACY AND HSR
;; ***************************
(defrule SYNERGIES::ATMS-atmospheric-temperature-HSR-accuracy
(REQUIREMENTS::Measurement (frequency# 183e9) (Id ?id1)(taken-by ?ins1))
(REQUIREMENTS::Measurement (Parameter "1.2.1 Atmospheric temperature") (frequency# 50e9) (Id ?id2) (taken-by ?ins2)(Horizontal-Spatial-Resolution# ?hsr))
(SYNERGIES::cross-registered (measurements $?meas&:(contains$ $?meas ?id1)&:(contains$ $?meas ?id2)) (degree-of-cross-registration spacecraft))
=>
(assert (REQUIREMENTS::Measurement (Parameter "1.2.1 Atmospheric temperature") (Accuracy# 1.25) (Vertical-Spatial-Resolution# 2000) (Horizontal-Spatial-Resolution# ?hsr) 
(Id (str-cat ?id1 "-syn-" ?id2)) (taken-by (str-cat ?ins1 "-syn-" ?ins2))))
)

(defrule SYNERGIES::ATMS-surface-temperature-HSR-accuracy
(REQUIREMENTS::Measurement (frequency# 183e9) (Id ?id1)(taken-by ?ins1))
(REQUIREMENTS::Measurement (Parameter "1.2.2 Air temperature at surface") (frequency# 50e9) (Id ?id2) (taken-by ?ins2)(Horizontal-Spatial-Resolution# ?hsr))
(SYNERGIES::cross-registered (measurements $?meas&:(contains$ $?meas ?id1)&:(contains$ $?meas ?id2)) (degree-of-cross-registration spacecraft))
=>
(assert (REQUIREMENTS::Measurement (Parameter "1.2.2 Air temperature at surface") (Accuracy# 1.25) (Horizontal-Spatial-Resolution# ?hsr) 
(Id (str-cat ?id1 "-syn-" ?id2)) (taken-by (str-cat ?ins1 "-syn-" ?ins2))))
)

(defrule SYNERGIES::ATMS-atmospheric-wind-speed-HSR-accuracy
(REQUIREMENTS::Measurement (frequency# 183e9) (Id ?id1)(taken-by ?ins1))
(REQUIREMENTS::Measurement (Parameter "1.4.1 atmospheric wind speed") (Id ?id2) (frequency# 50e9) (taken-by ?ins2)(Horizontal-Spatial-Resolution# ?hsr))
(SYNERGIES::cross-registered (measurements $?meas&:(contains$ $?meas ?id1)&:(contains$ $?meas ?id2)) (degree-of-cross-registration spacecraft))
=>
(assert (REQUIREMENTS::Measurement (Parameter "1.4.1 atmospheric wind speed") (Accuracy# 2.5) (Vertical-Spatial-Resolution# 2000) (Horizontal-Spatial-Resolution# ?hsr) 
(Id (str-cat ?id1 "-syn-" ?id2)) (taken-by (str-cat ?ins1 "-syn-" ?ins2))))
)

(defrule SYNERGIES::ATMS-air-wind-surface-HSR-accuracy
(REQUIREMENTS::Measurement (frequency# 183e9) (Id ?id1)(taken-by ?ins1))
(REQUIREMENTS::Measurement (Parameter "1.4.3 Air wind at surface") (Id ?id2) (frequency# 50e9) (taken-by ?ins2)(Horizontal-Spatial-Resolution# ?hsr))
(SYNERGIES::cross-registered (measurements $?meas&:(contains$ $?meas ?id1)&:(contains$ $?meas ?id2)) (degree-of-cross-registration spacecraft))
=>
(assert (REQUIREMENTS::Measurement (Parameter "1.4.3 Air wind at surface") (Accuracy# 2.5) (Horizontal-Spatial-Resolution# ?hsr) 
(Id (str-cat ?id1 "-syn-" ?id2)) (taken-by (str-cat ?ins1 "-syn-" ?ins2))))
)

(defrule SYNERGIES::ATMS-atmospheric-humidity-HSR-accuracy
(REQUIREMENTS::Measurement (Parameter "1.3.1 Atmospheric humidity") (Id ?id1) (frequency# 183e9) (taken-by ?ins1) (Horizontal-Spatial-Resolution# ?hsr))
(REQUIREMENTS::Measurement (Id ?id2) (frequency# 50e9) (taken-by ?ins2))
(SYNERGIES::cross-registered (measurements $?meas&:(contains$ $?meas ?id1)&:(contains$ $?meas ?id2)) (degree-of-cross-registration spacecraft))
=>
(assert (REQUIREMENTS::Measurement (Parameter "1.3.1 Atmospheric humidity") (Accuracy# 0.15) (Vertical-Spatial-Resolution# 2000) (Horizontal-Spatial-Resolution# ?hsr) 
(Id (str-cat ?id1 "-syn-" ?id2)) (taken-by (str-cat ?ins1 "-syn-" ?ins2))))
)

(defrule SYNERGIES::ATMS-air-humidity-surface-HSR-accuracy
(REQUIREMENTS::Measurement (Parameter "1.3.5 Air humidity at surface") (Id ?id1) (frequency# 183e9) (taken-by ?ins1) (Horizontal-Spatial-Resolution# ?hsr))
(REQUIREMENTS::Measurement (Id ?id2) (frequency# 50e9) (taken-by ?ins2))
(SYNERGIES::cross-registered (measurements $?meas&:(contains$ $?meas ?id1)&:(contains$ $?meas ?id2)) (degree-of-cross-registration spacecraft))
=>
(assert (REQUIREMENTS::Measurement (Parameter "1.3.5 Air humidity at surface") (Accuracy# 0.15) (Horizontal-Spatial-Resolution# ?hsr) 
(Id (str-cat ?id1 "-syn-" ?id2)) (taken-by (str-cat ?ins1 "-syn-" ?ins2))))
)

(defrule SYNERGIES::ATMS-cloud-liquid-water-HSR-accuracy
(REQUIREMENTS::Measurement (Parameter "1.7.1 Cloud liquid water") (Id ?id1) (frequency# 183e9) (taken-by ?ins1) (Horizontal-Spatial-Resolution# ?hsr))
(REQUIREMENTS::Measurement (Id ?id2) (frequency# 50e9) (taken-by ?ins2))
(SYNERGIES::cross-registered (measurements $?meas&:(contains$ $?meas ?id1)&:(contains$ $?meas ?id2)) (degree-of-cross-registration spacecraft))
=>
(assert (REQUIREMENTS::Measurement (Parameter "1.7.1 Cloud liquid water") (Accuracy# 0.2) (Vertical-Spatial-Resolution# 2000) (Horizontal-Spatial-Resolution# ?hsr) 
(Id (str-cat ?id1 "-syn-" ?id2)) (taken-by (str-cat ?ins1 "-syn-" ?ins2))))
)

(defrule SYNERGIES::ATMS-precipitation-rate-HSR-accuracy
(REQUIREMENTS::Measurement (Parameter "1.7.3 Precipitation rate") (Id ?id1) (frequency# 183e9) (taken-by ?ins1) (Horizontal-Spatial-Resolution# ?hsr))
(REQUIREMENTS::Measurement (Id ?id2) (frequency# 50e9) (taken-by ?ins2))
(SYNERGIES::cross-registered (measurements $?meas&:(contains$ $?meas ?id1)&:(contains$ $?meas ?id2)) (degree-of-cross-registration spacecraft))
=>
(assert (REQUIREMENTS::Measurement (Parameter "1.7.3 Precipitation rate") (Accuracy# 1.4) (Vertical-Spatial-Resolution# 2000) (Horizontal-Spatial-Resolution# ?hsr) 
(Id (str-cat ?id1 "-syn-" ?id2)) (taken-by (str-cat ?ins1 "-syn-" ?ins2))))
)

(defrule SYNERGIES::ATMS-precipitation-rate-surface-HSR-accuracy
(REQUIREMENTS::Measurement (Parameter "1.7.4 Precipitation rate at surface") (Id ?id1) (frequency# 183e9) (taken-by ?ins1) (Horizontal-Spatial-Resolution# ?hsr))
(REQUIREMENTS::Measurement (Id ?id2) (frequency# 50e9) (taken-by ?ins2))
(SYNERGIES::cross-registered (measurements $?meas&:(contains$ $?meas ?id1)&:(contains$ $?meas ?id2)) (degree-of-cross-registration spacecraft))
=>
(assert (REQUIREMENTS::Measurement (Parameter "1.7.4 Precipitation rate at surface") (Accuracy# 1.4) (Horizontal-Spatial-Resolution# ?hsr) 
(Id (str-cat ?id1 "-syn-" ?id2)) (taken-by (str-cat ?ins1 "-syn-" ?ins2))))
)

(defrule SYNERGIES::ATMS-tropical-storms-hurricane-HSR-accuracy
(REQUIREMENTS::Measurement (Parameter "1.7.5 Tropical storms and hurricanes") (Id ?id1) (frequency# 183e9) (taken-by ?ins1) (Horizontal-Spatial-Resolution# ?hsr))
(REQUIREMENTS::Measurement (Id ?id2) (frequency# 50e9) (taken-by ?ins2))
(SYNERGIES::cross-registered (measurements $?meas&:(contains$ $?meas ?id1)&:(contains$ $?meas ?id2)) (degree-of-cross-registration spacecraft))
=>
(assert (REQUIREMENTS::Measurement (Parameter "1.7.5 Tropical storms and hurricanes") (Vertical-Spatial-Resolution# 2000) (Horizontal-Spatial-Resolution# ?hsr) 
(Id (str-cat ?id1 "-syn-" ?id2)) (taken-by (str-cat ?ins1 "-syn-" ?ins2))))
)

(defrule CAPABILITIES::cross-register-measurements-from-cross-registered-instruments
	(SYNERGIES::cross-registered-instruments (instruments $?ins) (platform ?sat))
	?c <- (accumulate (bind ?str "")                        ;; initializer
                (bind ?str (str-cat ?str " " $?m1))                    ;; action
                ?str                                        ;; result
                (CAPABILITIES::Manifested-instrument (Name ?ins1&:(contains$ $?ins ?ins1)) (flies-in ?sat) (measurement-ids $?m1))
				) ;; CE
	=>
	(assert (SYNERGIES::cross-registered (measurements (explode$ ?c)) (degree-of-cross-registration spacecraft) (platform ?sat)))
	;(printout t ?c crlf)
)

