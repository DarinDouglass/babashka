(ns babashka.impl.env
  {:no-doc true}
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(defn- coerce
  [value]
  (try
    (let [parsed (edn/read-string value)]
      ;; We should never be parsing into a sybmol. As such, assume the value
      ;; was as it was meant to be before the EDN parsing.
      (if (symbol? parsed)
        value
        parsed))
    (catch Exception _
      value)))

(defn- has-path?
  [config [key & rest]]
  (if (contains? config key)
    (if (pos? (count rest))
      (has-path? (get config key) rest)
      true)))

(defn- ->path [env-var]
  (map (comp keyword #(str/replace % #"_" "-") str/lower-case)
       (str/split env-var #"__")))

(def config
  (let [file-config (-> "config.edn"
                        (io/resource)
                        (slurp)
                        (edn/read-string))]
    (reduce (fn [config [env-var value]]
              (let [path (->path env-var)]
                (cond-> config
                  (and (not (empty? path)) (has-path? config path))
                  (assoc-in path (coerce value)))))
            file-config
            (System/getenv))))
