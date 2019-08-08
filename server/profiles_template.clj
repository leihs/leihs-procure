; Copy this file to profiles.clj, if you intend to use the auto watch/reset.

{:profiles/dev {:aot ^:replace [],
                :dependencies [[org.clojure/tools.namespace "0.2.11"]],
                :repl-options {:init (require 'app)}}}
