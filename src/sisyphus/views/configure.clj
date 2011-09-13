(ns sisyphus.views.configure
  (:require [sisyphus.views.common :as common])
  (:require [noir.response :as resp])
  (:use [sisyphus.models.configuration :only
         [get-configuration update-configuration do-replication]])
  (:use noir.core hiccup.core hiccup.page-helpers hiccup.form-helpers))

(defpartial replication-results
  [res]
  (cond (nil? res) [:p "No data."]
        (:no_changes res) [:p "No changes."]
        :else
        (let [h (first (:history res))]
          [:ul
           [:li (format "Start time: %s" (:start_time h))]
           [:li (format "End time: %s" (:end_time h))]
           [:li (format "Start last seq: %s" (:start_last_seq h))]
           [:li (format "End last seq: %s" (:end_last_seq h))]
           [:li (format "Recorded seq: %s" (:recorded_seq h))]
           [:li (format "Missing checked: %s" (:missing_checked h))]
           [:li (format "Missing found: %s" (:missing_found h))]
           [:li (format "Docs read: %s" (:docs_read h))]
           [:li (format "Docs written: %s" (:docs_written h))]
           [:li (format "Doc write failures: %s" (:doc_write_failures h))]])))

(defpage [:post "/configure/replicate"] {}
  (let [[forward reverse] (do-replication)]
    (common/layout
     "Replication results"
     [:section#results
      [:div.page-header
       [:h1 "Replication results"]]
      [:div.row
       [:div.span4.columns [:h2 "Forward"]]
       [:div.span12.columns (replication-results forward)]]
      [:div.row
       [:div.span4.columns [:h2 "Reverse"]]
       [:div.span12.columns (replication-results reverse)]]])))

(defpage
  [:post "/configure/update"] {:as config}
  (update-configuration config)
  (resp/redirect "/configure"))

(defpage "/configure" {}
  (let [config (get-configuration)]
    (common/layout
     "Configure"
     [:section#configure
      [:div.page-header
       [:h1 "Configure"]]
      [:div.row
       [:div.span4.columns "&nbsp;"]
       [:div.span12.columns
        (form-to [:post "/configure/update"]
                 [:fieldset
                  [:legend "Remote database"]
                  [:div.clearfix
                   [:label {:for "remote-host"} "Host"]
                   [:div.input
                    [:input.xlarge {:id "remote-host" :name "remote-host" :size 30
                                    :type "text" :value (:remote-host config)}]]]
                  [:div.clearfix
                   [:label {:for "remote-port"} "Port"]
                   [:div.input
                    [:input.xlarge {:id "remote-port" :name "remote-port" :size 10
                                    :type "text" :value (:remote-port config)}]]]
                  [:div.clearfix
                   [:label {:for "remote-name"} "Name"]
                   [:div.input
                    [:input.xlarge {:id "remote-name" :name "remote-name" :size 30
                                    :type "text" :value (:remote-name config)}]]]
                  [:div.clearfix
                   [:label {:for "remote-username"} "Username"]
                   [:div.input
                    [:input.xlarge {:id "remote-username" :name "remote-username" :size 30
                                    :type "text" :value (:remote-username config)}]]]
                  [:div.clearfix
                   [:label {:for "remote-password"} "Password"]
                   [:div.input
                    [:input.xlarge {:id "remote-password" :name "remote-password" :size 30
                                    :type "password" :value (:remote-password config)}]]]
                  [:div.actions
                   [:input.btn.primary {:value "Update" :type "submit"}]]])]]
      [:div.row
       [:div.span4.columns "&nbsp;"]
       [:div.span12.columns
        (form-to [:post "/configure/replicate"]
                 [:div.actions
                  [:input.btn.danger {:value "Replicate" :type "submit"}]])]]])))
