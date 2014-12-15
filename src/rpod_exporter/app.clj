(ns rpod-exporter.app
  (:require [clojure.tools.cli :refer [parse-opts]]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [rpod-exporter.export.json :refer [export-to-file]]
            [rpod-exporter.extractor :refer [stream]])
  (:gen-class :main true))

(def cli-options
  [["-o" "--output=/path/to/export/folder" "A folder into which the exported data will be stored"
    :default "./export/"]
   ["-h" "--help"]])

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (string/join \newline errors)))

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn usage
  [options-summary]
  (->> ["A command-line utility to export podcast entries from RPOD.RU to Markdown format."
        ""
        "Usage: rpod-exporter --output=./export my-podcast"
        ""
        "where 'my-podcast' is an identifier of used as the domain name of your podcast like http://{my-podcast}.rpod.ru"
        ""
        "Options:"
        options-summary
        ""]
       (string/join \newline)))

(defn- file-name
  [url]
  (.getName (java.io.File. url)))

(defn- file-extension
  [url]
  (let [pos       (.lastIndexOf url ".")]
    (if (pos? pos) (subs url pos) "")))

(defn- download-file-to
  [to-path from-url]
  (with-open [in  (io/input-stream from-url)
              out (io/output-stream to-path)]
    (io/copy in out)))

(defn export-podcast
  [base-path {:keys [id] :as entry}]
  (let [path (str base-path "/" id)]
    (.mkdirs (java.io.File. path))
    ;; export entry to json
    (export-to-file (str path "/entry.json") entry)
    ;; download file itself
    (download-file-to (str path "/" (file-name (:download entry))) (:download entry))
    ;; download all images
    (doseq [[index image] (map-indexed vector (:images entry))]
      (let [from-url (:href image)]
        (download-file-to (str path "/" index (file-extension from-url)) from-url)))))

(defn -main
  [& args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    (cond
     (:help options)            (exit 0 (usage summary))
     (not= (count arguments) 1) (exit 1 (usage summary))
     errors                     (exit 1 (error-msg errors)))

    (let [export-folder (java.io.File. (:output options))]
      ;; check if the output file already exists
      (when (.exists export-folder)
        (exit 0 (str export-folder " already exists. Aborting.")))
      (.mkdirs export-folder))

    (doseq [entry (stream (str "http://" (first arguments) ".rpod.ru"))]
      (println "Exporting:" (:date entry))
      (export-podcast (:output options) entry))))
