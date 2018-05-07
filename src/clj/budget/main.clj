(ns budget.main
  (:require
   [compojure.handler :refer [site]]
   [org.httpkit.server :refer [run-server]]
   [budget.core :refer [my-app start-router!]]))

(defn -main [& args]
  (start-router!)
  (run-server (site #'my-app) {:port 1337})
  (println "Server up and running, port 1337."))
