(ns rpod-exporter.extractor
  (:require [net.cgrand.enlive-html :as enlive]))

(def source-date-format (-> (java.time.format.DateTimeFormatterBuilder.)
                          (.appendValue java.time.temporal.ChronoField/DAY_OF_MONTH)
                          (.appendLiteral " ")
                          (.appendText java.time.temporal.ChronoField/MONTH_OF_YEAR)
                          (.appendLiteral " ")
                          (.appendValue java.time.temporal.ChronoField/YEAR)
                          (.appendLiteral " ")
                          (.appendValue java.time.temporal.ChronoField/HOUR_OF_DAY)
                          (.appendLiteral ":")
                          (.appendValue java.time.temporal.ChronoField/MINUTE_OF_HOUR)
                          (.toFormatter (java.util.Locale. "ru" "RU"))))

(def target-date-format (-> (java.time.format.DateTimeFormatterBuilder.)
                          (.appendValue java.time.temporal.ChronoField/YEAR 4)
                          (.appendLiteral "-")
                          (.appendValue java.time.temporal.ChronoField/MONTH_OF_YEAR 2)
                          (.appendLiteral "-")
                          (.appendValue java.time.temporal.ChronoField/DAY_OF_MONTH 2)
                          (.appendLiteral " ")
                          (.appendValue java.time.temporal.ChronoField/HOUR_OF_DAY 2)
                          (.appendLiteral ":")
                          (.appendValue java.time.temporal.ChronoField/MINUTE_OF_HOUR 2)
                          (.toFormatter java.util.Locale/ENGLISH)))

(def simple-date-format (-> (java.time.format.DateTimeFormatterBuilder.)
                            (.appendValue java.time.temporal.ChronoField/YEAR 4)
                            (.appendValue java.time.temporal.ChronoField/MONTH_OF_YEAR 2)
                            (.appendValue java.time.temporal.ChronoField/DAY_OF_MONTH 2)
                            (.toFormatter java.util.Locale/ENGLISH)))

(defn- next-page-url
  [content]
  (when-let [link (first (enlive/select content [:.Navigator :#next_page]))]
    (get-in link [:attrs :href])))

(defn- title
  [post]
  (-> post
      (enlive/select [:.podcast_title :h1 :a enlive/text])
      (first)))

(defn- podcast-url
  [post]
  (-> post
      (enlive/select [:.podcast_title :h1 :a])
      (first)
      (get-in [:attrs :href])))

(defn- download-url
  [post]
  (-> post
      (enlive/select [:.podcast_title :h1 :.misc])
      (first)
      (get-in [:attrs :href])))

(defn- to-english-locale
  "Converts date string in ru locale to en"
  [date]
  (when date
    (-> date
      (java.time.LocalDateTime/parse source-date-format)
      (.format target-date-format))))

(defn- to-plain
  [date]
  (when date
    (-> date
        (java.time.LocalDateTime/parse source-date-format)
        (.format simple-date-format))))

(defn- podcast-date
  [post format-fn]
  (-> post
      (enlive/select [:.podcast_date :span enlive/text])
      (first)
      (format-fn)))

(defn- podcast-tags
  [post]
  (->> (enlive/select post [:.podcast_tags :a])
       (mapcat :content)
       (into [])))

(defn- podcast-images
  [post]
  (->> (enlive/select post [:.podcast_body :img])
       (mapv #(let [attrs (:attrs %)]
                {:title (:alt attrs)
                 :href  (:src attrs)}))))

(defn- to-original-url
  [rpod-away-link]
  (when rpod-away-link
      (when-let [url (second (re-find #"http://rpod.ru/away\?url=(.*)" rpod-away-link))]
        (java.net.URLDecoder/decode url))))

(defn- post-image-copyright
  [post]
  (when-let [copyright (-> post
                           (enlive/select [:.podcast_body :> :a])
                           (first))]
    {:origin (:content copyright)
     :url    (to-original-url (get-in copyright [:attrs :href]))}))

(defn- podcast-notes
  [post]
  {:notes     (into [] (filter string?
                               (enlive/select post [:.podcast_body :li enlive/text])))
   :copyright (post-image-copyright post)})

(defn- extract-post
  [post]
  {:id       (podcast-date post to-plain)
   :title    (title post)
   :date     (podcast-date post to-english-locale)
   :url      (podcast-url post)
   :download (download-url post)
   :images   (podcast-images post)
   :tags     (podcast-tags post)
   :content  (podcast-notes post)})

(defn stream
  [url]
  (lazy-seq
   (when-let [content (enlive/html-resource (java.net.URL. url))]
     (let [podcasts (map extract-post (enlive/select content [:.podcast]))]
       (if-let [next-page (next-page-url content)]
         (lazy-cat podcasts (stream next-page))
         podcasts)))))
