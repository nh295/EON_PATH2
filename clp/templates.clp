(deftemplate SYNERGIES::NUM-CHANNELS (multislot num-channels))

(deftemplate DATABASE::Revisit-time-of "Revisit time of an architecture-orbit-instrument tuple" 
   (slot mission-architecture) 
   (slot num-of-planes#)  
   (slot num-of-sats-per-plane#)  
   (slot orbit-altitude#) 
   (slot orbit-type) 
   (slot orbit-inclination) 
   (slot orbit-raan) 
   (slot instrument-field-of-view#) 
   (slot avg-revisit-time-global#) 
   (slot avg-revisit-time-tropics#) 
   (slot avg-revisit-time-northern-hemisphere#) 
   (slot avg-revisit-time-southern-hemisphere#) 
   (slot avg-revisit-time-cold-regions#) 
   (slot avg-revisit-time-US#) 
   )
(deftemplate SEARCH-HEURISTICS::improve-heuristic (slot id))

(deftemplate SEARCH-HEURISTICS::list-improve-heuristics (multislot list) (slot num-heuristics (default 0)))
(deftemplate SYNERGIES::cross-registered "Declare a set of measurements as cross-registered"
    (multislot measurements) (slot degree-of-cross-registration) (slot platform))
  
(deftemplate SYNERGIES::cross-registered-instruments "Declare a set of measurements as cross-registered"
    (multislot instruments) (slot degree-of-cross-registration) (slot platform)(slot total-num-channels (default 0)))

(deftemplate REASONING::partially-satisfied "Requirements that are partially satisfied" (slot subobjective)
    (slot objective) (slot parameter) (slot taken-by) (slot attribute) (slot required) (slot achieved))

(deftemplate REASONING::fully-satisfied "Requirements that are partially satisfied" (slot subobjective)
    (slot objective) (slot parameter) (slot taken-by))
(deftemplate REASONING::stop-improving "Flag to stop improving a measurement through application of synergy rules" (slot Measurement))

(deftemplate REASONING::architecture-eliminated "Reasons why architecture was eliminated" (slot arch-id) (slot fit) (slot arch-str) 
(slot benefit) (slot lifecycle-cost) (slot utility) (slot pareto-ranking) (slot programmatic-risk) (slot fairness) (slot launch-risk) (slot reason-id) 
(slot data-continuity) (slot discounted-value) (slot reason-str))
	
(deftemplate ASSIMILATION2::UPDATE-REV-TIME (slot parameter ) (slot avg-revisit-time-global#) (slot avg-revisit-time-US#))

(deftemplate AGGREGATION::STAKEHOLDER (slot id) (slot fuzzy-value) (slot parent) (slot index) (slot satisfaction) (slot satisfied-by) (multislot obj-fuzzy-scores) (multislot obj-scores) (slot reason) (multislot weights))
(deftemplate AGGREGATION::OBJECTIVE (slot id) (slot fuzzy-value) (slot index) (slot satisfaction) (slot reason) (multislot subobj-fuzzy-scores) (multislot subobj-scores) (slot satisfied-by) (slot parent) (multislot weights))
(deftemplate AGGREGATION::SUBOBJECTIVE (slot id) (slot fuzzy-value) (slot index) (slot satisfaction) (multislot attributes) (multislot attrib-scores) (multislot attrib-vals) (multislot reasons) (slot reason) (slot satisfied-by) (slot parent))
(deftemplate AGGREGATION::ATTRIBUTE (slot id) (slot fuzzy-value) (slot satisfaction) (slot reason) (slot satisfied-by) (slot parent))
(deftemplate AGGREGATION::VALUE (slot satisfaction) (slot fuzzy-value) (slot reason) (multislot weights) (multislot sh-scores) (multislot sh-fuzzy-scores))

(deftemplate REASONING::fuzzy-number (slot value) (slot value#) (slot type) (slot id) (multislot interval) (slot unit) (slot explanation))

	
(deftemplate ORBIT-SELECTION::orbit (slot orb) (slot of-instrument) (slot in-mission) (slot is-type) (slot h) (slot i) (slot e) (slot a) (slot raan) (slot anomaly) (slot penalty-var) )
(deftemplate ORBIT-SELECTION::launcher (slot lv) (multislot performance) (slot cost) (slot diameter) (slot height) )
	
(deftemplate CAPABILITIES::can-measure (slot instrument) (slot in-orbit) (slot orbit-type) (slot orbit-altitude#) (slot data-rate-duty-cycle#) (slot power-duty-cycle#) (slot data-rate-constraint) (slot orbit-inclination) (slot orbit-RAAN) (slot can-take-measurements) (slot reason))
(deftemplate CAPABILITIES::resource-limitations (slot mission) (multislot instruments) (slot data-rate-duty-cycle#) (slot power-duty-cycle#) (slot reason))
(deftemplate DOWN-SELECTION::MAX-COST (slot max-cost) )	
(deftemplate DOWN-SELECTION::MIN-SCIENCE (slot min-benefit))
(deftemplate DOWN-SELECTION::MIN-PARETO-RANK (slot min-pareto-rank))
(deftemplate DOWN-SELECTION::MIN-UTILITY (slot min-utility))