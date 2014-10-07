(ns fantasy-football-analyzer.core
  (:require [net.cgrand.enlive-html :as html]
            [clojure.string :as string]
            [clojure.tools.logging :as log]))

(defn scoreboard-url
  [team-id scoring-id]
  (str "http://games.espn.go.com/ffl/boxscorequick?leagueId=741108&teamId="
        team-id
        "&scoringPeriodId="
        scoring-id
        "&seasonId=2014&view=scoringperiod&version=quick"))

(defn fetch-url
  "Retrieve the html from a url"
  [url]
  (log/info "Fetched")
  (html/html-resource (java.net.URL. url)))

(def ^:dynamic scoreboard (fetch-url (scoreboard-url 1 1)))

(defn starter-table
  []
  (html/select scoreboard [:#playertable_0]))

(defn bench-table
  []
  (html/select scoreboard [:#playertable_1]))

(defn get-row-text
  "Gets the text"
  [row selector]
  (-> (html/select row selector)
      first
      html/text))

(defn position-matcher
  "Tests for the position"
  [test-string]
  (loop [pos [" QB", " WR", " RB", " TE", " K", " D/ST"]]
    (if (empty? pos)
      "nil"
      (if (> (.indexOf test-string (first pos)) -1)
        (string/trim (string/lower-case (first pos)))
        (recur (rest pos))))))

(defn get-position
  "Returns the position of the player"
  [row]
  (-> (html/select row [:.playertablePlayerName])
      first
      html/text
      (string/replace #"\u00A0" " ")
      position-matcher))

(defn get-name
  "Returns the name of the player"
  [row]
  (get-row-text row [:a]))

(defn get-points
  "Returns the amount of points for a player"
  [row]
  (let [score (get-row-text row [:.playertableStat])]
    (if (= "--" score) 0 (read-string score))))

(defn add-player
  "Add player to structure"
  [structure row]
  (let [position (get-position row)
        name (get-name row)
        points (get-points row)
        player [position name points]
        key (keyword (string/lower-case position))
        new-vector (conj (structure key) player)]
        (conj structure {key new-vector})))

(def base-structure
  {:qb [] :rb [] :wr [] :te [] :d/st [] :k []})

(defn build-structure
  "Add players to structure"
  [current-structure current-rows]
  (loop [rows current-rows
         structure current-structure]
         (if (empty? rows)
          structure
          (recur (rest rows) (add-player structure (first rows))))))

(defn get-all-players
  []
  (let [starters (build-structure base-structure (html/select (starter-table) [:.pncPlayerRow]))
        bench (build-structure starters (html/select (bench-table) [:.pncPlayerRow]))]
    bench))

(defn get-sorted
  "Get sorted of position"
  [structure position]
  (reverse (sort (map last (structure position)))))

(defn get-points-for-position
  "Get points for position"
  [structure position num-players]
  (reduce + (take num-players (get-sorted structure position))))

(defn get-flex-points
  "Get all the points for the flex"
  [structure num-flex]
  (->> (flatten [(drop 2 (get-sorted structure :rb))
                 (drop 2 (get-sorted structure :wr))
                 (drop 1 (get-sorted structure :te))])
        sort
        reverse
        (take num-flex)
        (reduce +)))


(defn get-optimized-points
  "Get the optimized team points"
  []
  (+ (get-points-for-position (get-all-players) :qb 1)
     (get-points-for-position (get-all-players) :rb 2)
     (get-points-for-position (get-all-players) :wr 2)
     (get-points-for-position (get-all-players) :te 1)
     (get-points-for-position (get-all-players) :d/st 1)
     (get-points-for-position (get-all-players) :k 1)
     (get-flex-points (get-all-players) 1)))

(defn get-actual-points
  "Get the actual points scored"
  []
  (read-string
    (html/text
      (first
        (html/select scoreboard [:.totalScore])))))

(defn get-team-name
  "Get the team name"
  []
  (html/text
    (first
      (html/select scoreboard [:#teamInfos :.bodyCopy :div]))))

(defn get-coaching-percentage
  []
  (str (format "%.1f" (* (/ (get-actual-points) (get-optimized-points)) 100)) "%"))

(defn print-team-stats
  "Print it all out"
  []
  (println (get-team-name))
  (println (get-optimized-points))
  (println (get-actual-points))
  (println (get-coaching-percentage)))

(defn print-team-for-week
  "Print team for week"
  [team-id week-id]
  (binding [scoreboard (fetch-url (scoreboard-url team-id week-id))]
    (print-team-stats)))

(defn -main
  "Main"
  [& args]
  (let [week-id (if (nil? (first args))
                      1
                      (read-string (first args)))]
    (print-team-for-week 1 week-id)
    (print-team-for-week 2 week-id)
    (print-team-for-week 3 week-id)
    (print-team-for-week 5 week-id)
    (print-team-for-week 6 week-id)
    (print-team-for-week 7 week-id)
    (print-team-for-week 8 week-id)
    (print-team-for-week 9 week-id)))
