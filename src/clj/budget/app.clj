(ns budget.app
  (:require
   [com.stuartsierra.component :as c]
   [compojure.handler :refer [site]]
   [budget.handler :as handler]))

(defrecord App [handler]
  c/Lifecycle

  (start [component]
    (if handler
      component
      (do
        (println ";; [App] Starting, attaching handler")
        (println ";; comp: " component)
        (assoc component :handler (site #'handler/my-app)))))

  (stop [component]
    (println ";; [App] Stopping")
    (println ";; comp: " component)
    (assoc component :handler nil)))

(defn new-app
  []
  (map->App {}))