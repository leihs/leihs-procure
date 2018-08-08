(ns leihs.procurement.backend.main
  (:gen-class)
  (:require [clojure.pprint :refer [pprint]]
            [clojure.tools.cli :as cli]
            [leihs.procurement.backend.run :as run]
            [logbug.thrown :as thrown]))

(thrown/reset-ns-filter-regex #"^(leihs|cider)\..*")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def cli-options [["-h" "--help"]])

(defn main-usage
  [options-summary & more]
  (->>
    ["Leihs PERM" "" "usage: leihs-perm [<opts>] SCOPE [<scope-opts>] [<args>]"
     "" "Options:" options-summary "" ""
     (when more
       ["-------------------------------------------------------------------"
        (with-out-str (pprint more))
        "-------------------------------------------------------------------"])]
    flatten
    (clojure.string/join \newline)))

(defn -main
  [& args]
  (let [{:keys [options arguments errors summary]}
          (cli/parse-opts args cli-options :in-order true)
        pass-on-args (->> [options (rest arguments)]
                          flatten
                          (into []))]
    (cond (:help options) (println (main-usage summary
                                               {:args args, :options options}))
          :else (case (-> arguments
                          first
                          keyword)
                  :run (apply run/-main (rest arguments))
                  (println (main-usage summary
                                       {:args args, :options options}))))))

;(-main "-h")
;(-main "run")
