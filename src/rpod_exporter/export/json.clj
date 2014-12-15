(ns rpod-exporter.export.json
  (:require [clojure.java.io :as io]
            [clojure.data.json :as json]))

(defn export-to-file
  [path entry]
  (let [output-file (io/file path)]
    (comment
      ;; check if the output file already exists
      (if (.exists output-file)
        (when (= "n" (prompt (str output-file " already exists. Do you want to overwrite it? [y/n]") #{"y" "n"}))
          (exit 0 "Aborting at user request."))
        (.createNewFile output-file))
      ;; check if the output file is accessible for writing
      (when (not (.canWrite output-file))
        (exit 1 (error-msg [(str output-file ": cannot be open for writing")]))))

    (with-open [w (io/writer path :append false :encoding "UTF-8")]
      (json/write entry w :escape-unicode false :escape-slash false))))
