(ns rpod-exporter.extractor
  (:require [net.cgrand.enlive-html :as enlive]))

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

(defn- podcast-date
  [post]
  (-> post
      (enlive/select [:.podcast_date :span enlive/text])
      (first)))

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
  {:notes     (into [] (enlive/select post [:.podcast_body :li enlive/text]))
   :copyright (post-image-copyright post)})

(defn- extract-post
  [post]
  {:title    (title post)
   :date     (podcast-date post)
   :url      (podcast-url post)
   :download (download-url post)
   :images   (podcast-images post)
   :tags     (podcast-tags post)
   :content  (podcast-notes post)})

(defn stream
  [url]
  (when-let [content (enlive/html-resource (java.net.URL. url))]
    (let [podcasts (enlive/select content [:.podcast])]
      (if-let [next-page (next-page-url content)]
        (lazy-cat (map extract-post podcasts) (stream next-page))
        podcasts))))
