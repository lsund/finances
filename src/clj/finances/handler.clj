(ns finances.handler
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.string :as string]
            [compojure.core :refer [GET POST routes]]
            [compojure.route :as route]
            [finances.db :as db]
            [finances.report :as report]
            [finances.util.core :as util]
            [finances.util.date :as util.date]
            [finances.views.budget :as views.budget]
            [finances.views.budget.transaction-group
             :as
             views.budget.transaction-group]
            [finances.views.calibrate-start-balances
             :as
             views.calibrate-start-balances]
            [finances.views.internal :as views]
            [finances.views.debts :as views.debts]
            [finances.views.delete-category :as views.delete-category]
            [finances.views.budget.manage-category :as views.budget.manage-category]
            [finances.views.reports :as views.reports]
            [finances.views.assets :as views.assets]
            [hiccup.page :refer [html5]]
            [medley.core :refer [map-keys]]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [ring.middleware.json :refer [wrap-json-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.util.response :refer [redirect]]
            [taoensso.timbre :as logging]
            [taoensso.timbre.appenders.core :as appenders]))

(logging/merge-config!
 {:appenders
  {:spit (appenders/spit-appender {:fname "data/finances.log"})}})

(def not-found (html5 "not found"))

(defn- app-routes [{:keys [db] :as config}]
  (routes
   (GET "/" [all]
        (views/render :budget
                      {:config config

                       :all?  (some? all)

                       :generate-report-div (db/monthly-report-missing? db
                                                                        config)}
                      (assoc (db/get-budget db config) :title "Budget")))
   (GET "/debts" []
        (views/render :debts config {:title "Debts"
                                     :debts (db/all db :debt)}))
   (GET "/reports" [id]
        (views/render :reports
                      config
                      {:title "Reports"
                       :report (when id
                                 (db/row db
                                         :report
                                         (util/parse-int id)))
                       :reports (db/all db :report)}))
   (GET "/stocks" []
        (views/render :assets
                      config
                      {:title "Stocks"
                       :assets
                       (db/all db :asset)
                       :transactions
                       (db/get-asset-transactions db :stock)}))
   (GET "/funds" []
        (views/render :assets
                      config
                      {:title "Funds"
                       :assets
                       (db/all db :asset)
                       :transactions
                       (db/get-asset-transactions db :fund)}))
   (GET "/budget/manage-category" [id]
        (views/render :manage-category
                      config
                      (assoc (db/get-budget db config)
                             :category
                             (db/row db :category (util/parse-int id))
                             :title
                             "Manage category")))
   (GET "/budget/transaction-group" [id]
        (views/render :transaction-group
                      config
                      {:title
                       "Transaction Group"

                       :transaction-group
                       (db/get-unreported-transactions db
                                                       config
                                                       (util/parse-int id))}))
   (GET "/delete-category" [id]
        (views/render :delete-category config {:title "Warning" :id
                                               id}))
   (POST "/calibrate-start-balances" []
         (views/render :calibrate
                       config
                       {:title
                        "Calibrate"
                        :total-start-balance
                        (db/get-total db :start_balance)
                        :categories
                        (->> (db/all db :category)
                             (sort-by :label)
                             (filter (comp not :hidden)))}))

   (POST "/add-category" [label funds]
         (db/add-category db
                          label
                          (util/parse-int funds))
         (redirect "/"))
   (POST "/add-debt" [label funds]
         (db/add db :debt {:label label :amount (util/parse-int funds)})
         (redirect "/debts"))
   (POST "/transfer/balance" [from to amount]
         (db/transfer-balance db
                              (util/parse-int from)
                              (util/parse-int to)
                              (util/parse-int amount))
         (redirect (str "/budget/manage-category?id=" from)))
   (POST "/transfer/start-balance" [from to amount]
         (db/transfer-start-balance db
                                    (util/parse-int from)
                                    (util/parse-int to)
                                    (util/parse-int amount))
         (redirect (str "/budget/manage-category?id=" from)))
   (POST "/transfer/both" [from to amount]
         (jdbc/with-db-transaction [t-db db]
           (db/transfer-balance t-db
                                (util/parse-int from)
                                (util/parse-int to)
                                (util/parse-int amount))
           (db/transfer-start-balance t-db
                                      (util/parse-int from)
                                      (util/parse-int to)
                                      (util/parse-int amount)))
         (redirect (str "/budget/manage-category?id=" from)))
   (POST "/spend" [id dec-amount]
         (db/add-transaction db
                             (util/parse-int id)
                             (util/parse-int dec-amount)
                             :decrement)
         (redirect "/"))
   (POST "/delete-category" [id]
         (db/delete-category db (util/parse-int id))
         (redirect "/"))
   (POST "/hide-category" [id]
         (db/update-row db :category {:hidden true} (util/parse-int id))
         (redirect "/"))
   (POST "/transaction/update-note" [id note]
         (db/update-row db :transaction {:note note} (util/parse-int id))
         (redirect "/"))
   (POST "/delete-transaction" [tx-id]
         (db/remove-transaction db (util/parse-int tx-id))
         (redirect "/"))
   (POST "/update-label" [id label]
         (db/update-label db
                          (util/parse-int id)
                          label)
         (redirect "/"))
   (POST "/update-start-balance" [id start-balance]
         (db/update-start-balance db
                                  (util/parse-int id)
                                  (util/parse-int start-balance))
         (redirect "/"))

   (POST "/assets/add-transaction" [id date buy shares rate total currency]
         (println id date buy shares rate total currency)
         (println "date " date)

         (db/add-asset-transaction db
                                   :assettransaction
                                   (util/parse-int id)
                                   date
                                   buy
                                   shares
                                   rate
                                   total
                                   currency)
         (let [asset (db/row db :asset (util/parse-int id))]
           (case (:type asset)
             1 (redirect "/stocks")
             (redirect "/funds"))))
   (POST "/assets/delete-transaction" [id]
         (db/delete db :assettransaction (util/parse-int id))
         (redirect "/assets"))
   (POST "/generate-report" req
         (report/generate config)
         (db/update-start-balances! db
                                    (map (fn [[x y]]
                                           {:id (util/parse-int x)
                                            :start-balance (util/parse-int y)})
                                         (map-keys #(-> %
                                                        str
                                                        (string/split #"-")
                                                        last)
                                                   (:params req))))
         (db/reset-month db)
         (redirect "/"))
   (POST "/merge-categories" [source-id dest-id]
         (db/merge-categories db
                              (util/parse-int source-id)
                              (util/parse-int dest-id))
         (redirect "/"))
   (route/resources "/")
   (route/not-found not-found)))

(defn new-handler
  [config]
  (-> (app-routes config)
      (wrap-json-params)
      (wrap-keyword-params)
      (wrap-params)
      (wrap-defaults
       (-> site-defaults (assoc-in [:security :anti-forgery] false)))))
