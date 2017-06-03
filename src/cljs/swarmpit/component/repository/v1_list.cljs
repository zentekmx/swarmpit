(ns swarmpit.component.repository.v1-list
  (:require [material.component :as comp]
            [clojure.walk :as walk]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.storage :as storage]
            [swarmpit.component.state :as state]
            [rum.core :as rum]
            [ajax.core :as ajax]))

(def cursor [:page :repository :list :v1 :data])

(def headers ["Name" "Description"])

(def render-item-keys
  [[:name] [:description]])

(defn- render-item
  [item]
  (let [value (val item)]
    value))

(defn- repository-handler
  [name query page]
  (ajax/GET "v1/repositories"
            {:headers {"Authorization" (storage/get "token")}
             :params  {:registryName    name
                       :repositoryQuery query
                       :repositoryPage  page}
             :handler (fn [response]
                        (let [res (walk/keywordize-keys response)]
                          (state/set-value res cursor)))}))

(rum/defc repository-list < rum/reactive [registry-name]
  (let [{:keys [results page limit total query]} (state/react cursor)
        offset (* limit (- page 1))
        repository (fn [index] (:name (nth results index)))]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (comp/panel-text-field
         {:hintText "Search in registry"
          :onChange (fn [_ v]
                      (repository-handler registry-name v 1))})]]
     (comp/mui
       (comp/table
         {:key         "tbl"
          :selectable  false
          :onCellClick (fn [i]
                         (dispatch! (str "/#/services/create/wizard/config?"
                                         "repository=" (repository i)
                                         "&registry=" registry-name)))}
         (comp/list-table-header headers)
         (comp/list-table-body results
                               render-item
                               render-item-keys)
         (if (not (empty? results))
           (comp/list-table-paging offset
                                   total
                                   limit
                                   #(repository-handler registry-name query (- (js/parseInt page) 1))
                                   #(repository-handler registry-name query (+ (js/parseInt page) 1))))))]))

(defn mount!
  [registry-name]
  (rum/mount (repository-list registry-name) (.getElementById js/document "content")))
