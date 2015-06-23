(ns pitchperfect.sound)

(def AudioContext (.-AudioContext js/window))

(defn set-timeout [time-ms callback]
  (.setTimeout js/window callback time-ms))

(defn set-volume [{gain :gain} value]
  (set! (-> gain .-gain .-value) value))

(defn set-frequency [{oscillator :oscillator} value]
  (set! (-> oscillator .-frequency .-value) value))

(defn play [player freq volume time-ms]
  (set-frequency player freq)
  (set-volume player volume)
  (set-timeout time-ms (fn []
                         (set-frequency player 0)
                         (set-volume player 0))))


(defn new-player []
  (let [ctx (new AudioContext)
        oscillator (.createOscillator ctx)
        gain (.createGain ctx)
        destination (.-destination ctx)
        player {:ctx ctx
                :gain gain
                :oscillator oscillator
                :destination destination}]

    (.connect oscillator gain)
    (.connect gain destination)
    (set-volume player 0)

    (.start oscillator)
    player))
