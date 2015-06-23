(ns pitchperfect.test-runner
  (:require
   [cljs.test :refer-macros [run-tests]]
   [pitchperfect.core-test]))

(enable-console-print!)

(defn runner []
  (if (cljs.test/successful?
       (run-tests
        'pitchperfect.core-test))
    0
    1))
