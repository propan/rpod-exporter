(ns rpod-exporter.app
  (:require [clojure.tools.cli :refer [parse-opts]]
            [clojure.string :as string]
            [rpod-exporter.extractor :refer [stream]])
  (:gen-class :main true))

(def cli-options
  [["-o" "--output=/path/to/export/folder" "A folder into which the exported data will be stored"
    :default "./export/{podcast-id}"]
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

(defn -main
  [& args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    (cond
     (:help options)            (exit 0 (usage summary))
     (not= (count arguments) 1) (exit 1 (usage summary))
     errors                     (exit 1 (error-msg errors)))

    (stream (str "http://" (first arguments) ".rpod.ru"))))
