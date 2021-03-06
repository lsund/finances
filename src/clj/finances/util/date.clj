(ns finances.util.date)

(def ^{:private true} date-string "yyyy-MM-dd")

(defn string->localdate [s]
  (java.time.LocalDate/parse s (java.time.format.DateTimeFormatter/ofPattern date-string)))

(defn ->localdate
  [date]
  {:pre [(not (nil? date))]}
  (condp = (type date)
    java.sql.Timestamp (.. date toLocalDateTime toLocalDate)
    java.sql.Date (.toLocalDate date)
    java.time.LocalDate date
    java.time.LocalDateTime date
    java.lang.String (string->localdate date)
    (throw (Exception. (str "Unknown date type: " (type date))))))

(defn fmt-date [d]
  (.format (java.time.format.DateTimeFormatter/ofPattern date-string)
           (->localdate d)))


(defn fmt-today [] (fmt-date (java.time.LocalDateTime/now)))

(defn past?
  ([d]
   (past? d (java.time.LocalDateTime/now)))
  ([d date]
   (>= (.getDayOfMonth date) d)))

(defn today [] (java.time.LocalDateTime/now))

(defn finances-month [day]
  (let [now (java.time.LocalDateTime/now)]
    (if (past? day now)
      (.getMonth (.plusMonths now 1))
      (.getMonth now))))


(defn finances-period [day]
  (let [n (.getValue (finances-month day))]
    [(java.time.LocalDate/of 2018 (- n 1) day) (java.time.LocalDate/of 2018 n day)]))


(defn get-current-date-header [day]
  (let [now (java.time.LocalDateTime/now)
        month (finances-month day)]
    (format "%s %s - %s %s %s"
            day
            (.plus month -1)
            day
            month
            (.getYear now))))
