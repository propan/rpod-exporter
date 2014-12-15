(ns rpod-exporter.export.json
  (:require [clojure.java.io :as io]
            [clojure.data.json :as json]))

(defn export-to-file
  [path entry]
  (let [output-file (io/file path)]
    (with-open [w (io/writer path :append false :encoding "UTF-8")]
      (json/write entry w :escape-unicode false :escape-slash false))))
