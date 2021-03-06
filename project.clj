(defproject finances "3.7.0-SNAPSHOT"
  :description "Fun finance program"
  :url "https://github.com/lsund/finances"
  :min-lein-version "2.7.0"
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.9.946"]
                 [org.clojure/core.async  "0.4.474"]
                 [org.clojure/tools.namespace "0.2.11"]
                 [org.clojure/java.jdbc "0.7.6"]
                 [org.postgresql/postgresql "42.2.2"]
                 [clojure.jdbc/clojure.jdbc-c3p0 "0.3.3"]
                 [environ "1.0.0"]
                 [http-kit "2.2.0"]
                 [ring/ring-defaults "0.3.0"]
                 [ring/ring-json "0.4.0"]
                 [compojure "1.6.1"]
                 [reagent "0.8.0"]
                 [hiccup "1.0.5"]
                 [me.raynes/fs "1.4.6"]
                 [com.taoensso/timbre "4.10.0"]
                 [io.aviso/pretty "0.1.34"]
                 [com.stuartsierra/component "0.3.2"]
                 [slingshot "0.12.2"]
                 [me.lsund/util "0.6.0"]]
  :plugins [[environ/environ.lein "0.3.1"]
            [io.aviso/pretty "0.1.34"]]
  :source-paths ["src/clj"]
  :uberjar-name "finances-standalone.jar"
  :ring {:handler finances.core/new-handler}
  :main finances.main
  :figwheel {:css-dirs ["resources/public/css"]}
  :repl-options {:init-ns user
                 :timeout 120000}
  :profiles {:dev {:dependencies [[binaryage/devtools "0.9.9"]]
                   :source-paths ["src/clj" "dev"]
                   :clean-targets ^{:protect false}
                   ["resources/public/js/compiled" :target-path]}})
