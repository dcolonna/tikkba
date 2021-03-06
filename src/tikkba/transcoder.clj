(ns tikkba.transcoder
  (:require [clojure.java.io :as io])
  (:import [org.apache.batik.dom AbstractDocument]
           [org.apache.batik.transcoder.image PNGTranscoder JPEGTranscoder]
           [org.apache.batik.transcoder TranscoderInput TranscoderOutput]))

(def png-transcoder-hints {:height PNGTranscoder/KEY_HEIGHT
                           :width PNGTranscoder/KEY_WIDTH})

(def default-png-options {})

(def jpeg-transcoder-hints {:quality JPEGTranscoder/KEY_QUALITY
                            :height JPEGTranscoder/KEY_HEIGHT
                            :width JPEGTranscoder/KEY_WIDTH})

(def default-jpeg-options {:quality 1})

(defprotocol Rasterizable
  "Protocol that defines methods that allows to convert SVG doc into raster image"
  (to-png [this output-path] [this output-path options] "Converts SVGDocument into PNG rasterimage")
  (to-jpeg [this output-path] [this output-path options] "Convert SVGDocument into JPEG rasterimage"))


(defn- apply-transcoder-options [transcoder hints-map options]
  "Function that applies supported options to the transcoder.
  Returns: transcoder object"
  (doseq [[option value] options]
    (when-let [opt-key (-> option keyword hints-map)]
      (.addTranscodingHint transcoder opt-key (float value))))
  transcoder)

(defn- transcode-doc [transcoder doc output-path]
  "Transcodes svg-document into raster-image.
  Returns: string of a output path"
  (with-open [ostream (io/output-stream output-path)]
    (.transcode transcoder
                (TranscoderInput. doc) 
                (TranscoderOutput. ostream)))
  output-path)

(extend-type AbstractDocument
  Rasterizable
  (to-png 
    ([doc output-path]
      (transcode-doc (PNGTranscoder.) doc output-path))
    ([doc output-path options]
      (let [transcoder (PNGTranscoder.)
            options (merge default-png-options options)]
        (-> transcoder 
          (apply-transcoder-options png-transcoder-hints options)
          (transcode-doc doc output-path)))))

  (to-jpeg 
    ([doc output-path]
      (let [transcoder (JPEGTranscoder.)]
        (-> transcoder
          (apply-transcoder-options jpeg-transcoder-hints default-jpeg-options)
          (transcode-doc doc output-path))))
    ([doc output-path options]
      (let [transcoder (JPEGTranscoder.)
            options (merge default-jpeg-options options)]
        (-> transcoder
          (apply-transcoder-options jpeg-transcoder-hints options)
          (transcode-doc doc output-path))))))

