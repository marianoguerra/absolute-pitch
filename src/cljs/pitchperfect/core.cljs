(ns pitchperfect.core
  (:require
    [pitchperfect.sound :as sound]
    [om.core :as om :include-macros true]
    [om-tools.dom :as dom :include-macros true]
    [om-tools.core :refer-macros [defcomponent]]))

(enable-console-print!)

(def note-base-offset {"A" 1
                       "B" 3
                       "C" 4
                       "D" 6
                       "E" 8
                       "F" 9
                       "G" 11})

(def key-freqs
  (into {}
        (for [n (range 1 88)]
          ; https://en.wikipedia.org/wiki/Piano_key_frequencies
          [n (* (.pow js/Math 1.0594630943592953 (- n 49)) 440) ])))

(defonce app-state (atom {:notes []
                          :hits 0
                          :miss 0
                          :playing? true}))

(def player (sound/new-player))

(defn note-to-hz [note octave]
  (let [base-num (get note-base-offset note)
        index (+ base-num (* 12 (dec octave)))
        freq (get key-freqs index)]
    freq))

(defn play-note [player [note-name octave] volume duration]
  (sound/play player (note-to-hz note-name octave) volume duration))

(defn note-hit [note app-state]
  (.log js/console "hit!" note)
  (swap! app-state (fn [state]
                     (let [{notes :notes hits :hits} state
                           new-notes (rest notes)]
                       (assoc state :notes new-notes :hits (inc hits)))))
  (play-note player note 0.8 300))

(defn note-miss [note app-state]
  (.log js/console "miss!" note)
  (swap! app-state (fn [state]
                     (let [{miss :miss} state]
                       (assoc state :miss (inc miss)))))
  (play-note player note 0.2 300))

(defn on-note-push [name]
  (fn [_]
    (let [{notes :notes} @app-state
          [index note] (first notes)
          note-name (first note)]
      (.log js/console "press" name "vs" note-name)

      (if (= note-name name)
        (note-hit note app-state)
        (note-miss note app-state)))))

(defn ctrl-button [name]
  (dom/li {:href "#" :class "button ctrl-button" :on-click (on-note-push name)} name))

(defn ctrl-buttons [names]
  (dom/ul {:class "stack button-group ctrl-buttons"}
          (map #(ctrl-button %) names)))

(def all-notes [["A" 4] ["B" 4] ["C" 4] ["D" 4] ["E" 4] ["F" 4] ["G" 4]
                ["A" 5] ["B" 5] ["C" 5] ["D" 5] ["E" 5] ["F" 5] ["G" 5]
                ["A" 6] ["B" 6] ["C" 6]])

(def line-notes #{["A" 4] ["C" 4] ["A" 6] ["C" 6]})

(def note-info
  (into {} (map-indexed (fn [i note-name]
                   [note-name
                    [(- 356 (* i 20))
                     (if (contains? line-notes note-name)
                       "note-line"
                       "note")]]) all-notes)))

(defn note-name-to-height [note-name]
  (get note-info note-name))

(defn pos-x-to-x [pos-x]
  (+ 128 (* pos-x 32)))

(defn note [note-data]
  (let [[pos-x note-name] note-data
        [y class-name] (note-name-to-height note-name)
        x (pos-x-to-x pos-x)]
    (dom/span {:class class-name :style {:top y :left x}})))

(defn score [notes]
  (dom/div {:class "lines-wrapper"}
           (dom/div {:class "lines"}
                    (map note notes))))

(defn update-notes [notes]
  (into [] (map (fn [[pos note-name]]
                    [(dec pos) note-name]) notes)))

(defn random-note []
  (get all-notes (rand-int (count all-notes))))

(defn tick []
    (swap! app-state
           (fn [state]
             (let [notes (:notes state)
                   active-notes (filter (fn [[i nname]] (> i 0)) notes)
                   max-note-idx (apply max (map first notes))
                   updated-notes (update-notes active-notes)
                   new-notes (if (or (nil? max-note-idx) (< max-note-idx 10))
                               (conj updated-notes [12 (random-note)])
                               updated-notes)]
               (assoc state :notes new-notes)))))

(defn start-loop []
  (.setInterval js/window tick 1000))

(defn game-stats [app]
  (dom/div {:class "row game-stats"}
           (dom/div {:class "small-6 columns"}
                    "Hits " (dom/span {:class "game-stat"} (:hits app)))
           (dom/div {:class "small-6 columns"}
                    "Miss " (dom/span {:class "game-stat"} (:miss app)))))

(defn main []
  (start-loop)
  (om/root
    (fn [app owner]
      (reify
        om/IRender
        (render [_]
          (dom/div {:class "row main-container"}
                   (dom/div {:class "small-2 columns main-columns col-left"}
                            (ctrl-buttons ["A" "B" "C"]))
                   (dom/div {:class "small-8 columns main-columns"}
                            (score (:notes app))
                            (game-stats app))
                   (dom/div {:class "small-2 columns main-columns col-right"}
                             (ctrl-buttons ["D" "E" "F" "G"]))))))
    app-state
    {:target (. js/document (getElementById "app"))}))

